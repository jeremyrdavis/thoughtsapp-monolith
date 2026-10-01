---
name: quarkus-persistence
description: >
  Use when writing or modifying Hibernate ORM with Panache code in a Quarkus app — defining entities extending
  PanacheEntity or PanacheEntityBase, writing PanacheRepository implementations, applying @Transactional,
  @Version, or @Embeddable, configuring datasources in application.properties, or seeding via import.sql.
  Trigger on @Entity, @Embeddable, @Transactional, listAll / findById / persist, or quarkus.datasource.* config.
  Excludes pattern choice (active record vs repository) — that's an architectural decision, covered elsewhere.
---

# Quarkus Persistence Skill

Conventions for Hibernate ORM with Panache. The mechanics: which Panache base class, where `@Transactional`
goes, what to do with value objects, how Dev Services fits in. The architectural choice of *active record vs
repository pattern* is a separate decision and lives elsewhere — this skill assumes the choice has been made
and shows the right Quarkus mechanics for either path.

**Foundational principle.** Transaction boundaries are use-case boundaries, not data-access boundaries. A `@Transactional` block protects the aggregate's invariants and the row lock — wrap exactly that, not I/O. Email send, Kafka publish, Prometheus counter, HTTP call: never inside the transaction. `REQUIRES_NEW` around I/O is an empty transaction wrapping nothing — it doesn't shorten any DB lock, because there's no DB work to be locked.

**Red Flags — STOP if you find yourself thinking:**

- About to put `@Transactional` on a `PanacheRepository` method.
- About to put `@Transactional` on a `private` method (CDI proxies don't intercept private calls).
- About to wrap I/O — SMTP, HTTP, Kafka, file system — inside a `@Transactional` method.
- About to use `REQUIRES_NEW` to "split a use case into smaller transactions for isolation."
- About to return a JPA entity from a JAX-RS resource method.
- Calling Panache statics (`Order.listAll()`) and an injected `OrderRepository` for the same entity in the same class.

If any of these surface, re-read Core Rules and Excuse / Reality before typing.

---

## When to Use

- Defining a new JPA entity (`@Entity`) or value type (`@Embeddable`).
- Writing a `PanacheRepository<T>` or extending a `PanacheEntity`.
- Applying `@Transactional` on a service method.
- Adding `@Version` for optimistic locking.
- Configuring a datasource in `application.properties` or seeding test data via `import.sql`.
- Reviewing a diff that puts `@Transactional` on a repository method, calls Panache statics inside an injected
  repository, or returns a lazy-loaded entity from a JAX-RS resource.

**Out of scope**: choosing between active record and repository style (architectural decision), aggregate
boundaries, where repository interfaces live in the package layout (your team's conventions), Flyway /
Liquibase migrations.

---

## Core Rules

1. **Pick one ID strategy per project and stick to it.** `PanacheEntity` (gives you a `Long id` for free) is
   fine for simple cases. `PanacheEntityBase` lets you declare your own `@Id` (e.g. UUID, typed ID wrapper) —
   pick this when the ID is part of the domain.
2. **Map value objects with `@Embeddable` + `@Embedded`.** A value object is a Java record (or a class with no
   identity). Annotate the type `@Embeddable`, embed it in the entity with `@Embedded`. Don't promote value
   objects to entities just to give them a row.
3. **Put `@Transactional` on the application service method, not on repositories or entities.** The service
   defines the transaction boundary; the repository is just persistence access. Multiple repository calls in
   one service method should run in one transaction.
4. **Use `@Version` on aggregate roots that need optimistic locking.** A `Long` field with `@Version` is enough.
   Hibernate increments it; concurrent updates throw `OptimisticLockException`, which an `ExceptionMapper`
   should translate to 409 Conflict.
5. **Don't mix Panache static calls and an injected `PanacheRepository<T>` for the same entity.** Pick one
   style per entity. If you've injected a repository, never call `Order.listAll()` in the service — go through
   the repository.
6. **Don't return lazy-loaded entities to the REST layer.** Either fetch eagerly inside the transaction, or
   (preferred) map to a DTO inside the application service before returning. Lazy fields outside a transaction
   throw `LazyInitializationException`.
7. **Leave `application.properties` empty for the datasource in dev/test.** Quarkus Dev Services will start a
   PostgreSQL container automatically. Only configure `quarkus.datasource.*` for production profiles.
8. **`import.sql` is for dev/test seed data, not migrations.** It runs in dev and test by default. For schema
   evolution, add Flyway or Liquibase — don't repurpose `import.sql`.

---

## Canonical Example

### Aggregate root with an embedded value object

```java
package com.example.orders.domain;

import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntityBase {

    @Id
    public UUID id;

    @Embedded
    public Money total;

    @Version
    public Long version;

    @Enumerated(EnumType.STRING)
    public Status status;

    public enum Status { PLACED, CANCELLED, FULFILLED }

    protected Order() {}

    public static Order place(UUID id, Money total) {
        Order o = new Order();
        o.id = id;
        o.total = total;
        o.status = Status.PLACED;
        return o;
    }

    public void cancel() {
        if (status == Status.FULFILLED) {
            throw new IllegalStateException("cannot cancel a fulfilled order");
        }
        this.status = Status.CANCELLED;
    }
}
```

### Value object as an `@Embeddable` record

```java
package com.example.orders.domain;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Currency;

@Embeddable
public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount == null || currency == null) throw new IllegalArgumentException();
        if (amount.signum() < 0) throw new IllegalArgumentException("amount must be non-negative");
    }
}
```

### Repository — Panache style

```java
package com.example.orders.infrastructure;

import com.example.orders.domain.Order;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.UUID;

@ApplicationScoped
public class OrderRepository implements PanacheRepositoryBase<Order, UUID> {

    public java.util.Optional<Order> findByIdOptional(UUID id) {
        return PanacheRepositoryBase.super.findByIdOptional(id);
    }
}
```

`PanacheRepositoryBase<E, ID>` is the variant for non-`Long` IDs. Use `PanacheRepository<E>` when the ID is
`Long` (i.e. the entity extends `PanacheEntity`).

### Application service — transaction boundary

```java
package com.example.orders.application;

import com.example.orders.domain.Order;
import com.example.orders.infrastructure.OrderRepository;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class OrderApplicationService {

    @Inject OrderRepository orders;

    @Transactional
    public UUID placeOrder(PlaceOrderRequest request) {
        Order order = Order.place(UUID.randomUUID(), request.total());
        orders.persist(order);
        Log.infof("order placed id=%s", order.id);
        return order.id;
    }
}
```

What this demonstrates:

- `PanacheEntityBase` because the ID is a domain `UUID`, not an arbitrary `Long`.
- `@Embeddable record Money` validates in the canonical constructor — value-object invariants enforced at
  construction.
- `@Version` on the root for optimistic locking.
- `@Transactional` on the service method only.
- Repository is `@ApplicationScoped` and implements `PanacheRepositoryBase<Order, UUID>` — no static calls.

---

## Anti-patterns

| Don't | Why it's wrong | Fix |
|---|---|---|
| `@Transactional` on a `PanacheRepository` method | Transaction boundary belongs at the use case (application service), not the data access. Causes nested transactions and unclear scope. | Move `@Transactional` to the service method that calls the repository. |
| Mixing `Order.listAll()` and `orderRepository.listAll()` in the same service | Two access styles for the same entity makes invariants and testing harder. | Pick one. If you injected a repository, go through it everywhere. |
| Returning the `Order` entity from a JAX-RS resource | Lazy fields throw outside the transaction; serializes the persistence model into the wire format. | Map to a DTO (record) in the application service; return the DTO. |
| `quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:...` in `application.properties` for dev | Defeats Dev Services. Now every developer needs a local Postgres. | Leave it empty in dev/test. Configure under a `%prod` profile when needed. |
| Using `import.sql` for production schema | It runs in dev/test only by default and isn't versioned the way migrations need to be. | Add Flyway (`quarkus-flyway`) or Liquibase for schema evolution. |
| `@Embeddable` class with setters and a no-arg constructor | Value object loses its identity-by-value semantics; mutability lets the entity escape ownership. | Use a Java record. Validate in the canonical constructor. |
| `@Entity public class OrderLine extends PanacheEntity { ... }` for a thing with no domain identity | Promotes a value object to an entity unnecessarily, requiring a synthetic ID. | Make it `@Embeddable` (or `@ElementCollection` if it's a list inside the aggregate). |
| `findById(id).orElseThrow()` returning a domain entity to the resource | Same lazy-loading hazard. | Return a DTO; throw a domain exception that an `ExceptionMapper` translates. |

---

## Excuse / Reality

When you catch yourself reasoning around the rules above, look here before you type. The left column is verbatim — what you'll actually say in your head or in Slack. The right column is what defeats it.

| Excuse | Reality |
|---|---|
| "One-line `@Transactional` on the repository unblocks CI; we'll refactor Monday." | Monday-you doesn't refactor named anti-patterns in the release branch. The anti-pattern is shipped, blessed by green CI, and harder to remove than to never add. Fix the boundary now. |
| "`@TestTransaction` makes the test pass — that proves it works." | It proves the *test* runs in a transaction. The production bug remains; the next caller of the same service hits the same `TransactionRequiredException`. Don't let test scaffolding mask production bugs. |
| "`@Transactional` on the repository is just defensive — it can't hurt." | Two services calling the same repository now disagree about where their transaction ends. Nested transactions and ambiguous boundaries cause the bugs they look like they're preventing. |
| "Lazy loading is fine because Quarkus has Jackson — the frontend just needs all the fields." | Jackson serializes outside the transaction. `LazyInitializationException` at row 47 of the JSON is the result. Map to a DTO inside the service while the session is alive. |
| "Schema drift is rare; `import.sql` plus drop-and-create is fine for staging." | Once QA or a customer has data in the schema, drop-and-create is a destructive change. Adopt Flyway/Liquibase the first day staging gets touched. |
| "Smaller transactions = better isolation. Splitting the use case into three `REQUIRES_NEW` methods is textbook hygiene." | A transaction's job is to protect the aggregate's invariants and the row lock — wrapping `emailNotifier.send` in `REQUIRES_NEW` is an empty transaction wrapping I/O, not isolation. There's no DB work in those nested transactions, so they hold no locks and shorten nothing. The lock contention is solved the moment the I/O leaves the persist transaction. |
| "If we split it, we still have the order even if email fails." | True — but the way to get that property is "send the email *after* the persist transaction commits," typically via an event observed at `AFTER_SUCCESS` or by calling the I/O method after the `@Transactional` method returns. Splitting a single use case into three `REQUIRES_NEW` transactions opens three connections to do what one transaction + one event handler does cleanly. |
| "The platform's static-analysis gate / audit committee mandates `@Transactional` at the persistence layer — 40 services already comply, and the audit committee blocks the next compliance window if even one is non-compliant." | A static check that mandates an anti-pattern is a broken check. Fix the gate (allowlist services with `@Transactional` at the application-service boundary, or have it walk call sites to verify each `persist`/`delete` is reached from inside a transaction), don't bend 40 codebases to match a misaimed linter. Compliance-theater that requires the wrong design is a control failure, not a control — and a `WriteGate` wrapper class that exists only to host `@Transactional` for the static analyzer is the same anti-pattern with extra files: the use-case boundary is still in the wrong place. |

---

## Quick Reference

### Choose the base class

| Use | When |
|---|---|
| `PanacheEntity` | You're fine with a generated `Long id` (rapid prototypes, simple tables). |
| `PanacheEntityBase` | You want to declare your own `@Id` — UUID, typed ID, composite key. Required when the ID is a typed domain value. |
| `PanacheRepository<T>` | Repository pattern, ID is `Long`. |
| `PanacheRepositoryBase<T, ID>` | Repository pattern, ID is anything else. |

### Annotation cheat sheet

| Annotation | On | Purpose |
|---|---|---|
| `@Entity` | Class | Marks a JPA entity; required on every persistent class. |
| `@Table(name=...)` | Class | Override the table name (default is the class name). |
| `@Id` | Field | Primary key. |
| `@Version` | Field (Long) | Optimistic lock; Hibernate manages it. |
| `@Embeddable` | Class | Marks a value type; gets inlined into owning entity's table. |
| `@Embedded` | Field | Embeds an `@Embeddable` value into the entity. |
| `@ElementCollection` | Field (Collection) | Persist a collection of value types in a side table. |
| `@Enumerated(EnumType.STRING)` | Field (enum) | Persist enum as a string (always prefer over ordinal). |
| `@Transactional` | Application service method | Defines the transaction boundary. |

### Dev Services and config

| Concern | Default behavior | Override |
|---|---|---|
| Datasource in dev/test | Quarkus starts PostgreSQL container | `%prod.quarkus.datasource.jdbc.url=...` |
| Schema generation in dev | `quarkus.hibernate-orm.database.generation=drop-and-create` (default in dev) | Set per profile in `application.properties` |
| Seed data in dev/test | `src/main/resources/import.sql` runs after schema generation | Use Flyway/Liquibase for prod migrations |
| Continuous testing | Real Dev-Services Postgres available in `@QuarkusTest` | (No override needed) |
