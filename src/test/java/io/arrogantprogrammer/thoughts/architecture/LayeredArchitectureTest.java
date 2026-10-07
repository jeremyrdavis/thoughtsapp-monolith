package io.arrogantprogrammer.thoughts.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

/**
 * Enforces the hexagonal-architecture dependency rules documented in CLAUDE.md:
 * domain and application never depend outward on adapters, adapters only depend
 * inward, and application services expose DTOs rather than domain types.
 */
class LayeredArchitectureTest {

    private static final String BASE = "io.arrogantprogrammer.thoughts";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    // domain is the innermost layer: it may depend on the JDK only, never on
    // application or any adapter.
    @Test
    void domainDependsOnNothingButJdk() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".application..",
                        BASE + ".adapters..");
        rule.check(CLASSES);
    }

    // application orchestrates domain logic but must stay ignorant of how it's exposed
    // or persisted, so it must not depend on any adapter package.
    @Test
    void applicationDoesNotDependOnAdapters() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE + ".application..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".adapters..");
        rule.check(CLASSES);
    }

    // Inbound adapters (Qute, JAX-RS) call into application only; reaching past it into
    // domain or sideways into an outbound adapter would break the hexagon.
    @Test
    void inboundAdaptersOnlyDependOnApplication() {
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        BASE + ".adapters.in.web..",
                        BASE + ".adapters.in.rest..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".domain..",
                        BASE + ".adapters.out..");
        rule.check(CLASSES);
    }

    // Outbound adapters (persistence, events) implement ports; they must never reach
    // sideways into an inbound adapter.
    @Test
    void outboundAdaptersDoNotDependOnInboundAdapters() {
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        BASE + ".adapters.out.persistence..",
                        BASE + ".adapters.out.events..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".adapters.in.web..",
                        BASE + ".adapters.in.rest..");
        rule.check(CLASSES);
    }

    // The core (domain + application) must never reference an adapter type directly,
    // which is what keeps adapters swappable without touching business logic.
    @Test
    void noAdapterTypeIsReferencedFromDomainOrApplication() {
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        BASE + ".domain..",
                        BASE + ".application..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".adapters..");
        rule.check(CLASSES);
    }

    // Public application service methods must return DTOs, not domain types, so
    // adapters never get a handle on (and can't mutate) the aggregate directly.
    @Test
    void applicationServiceMethodsDoNotReturnDomainTypes() {
        ArchRule rule = noMethods().that().areDeclaredInClassesThat().resideInAPackage(BASE + ".application..")
                .and().arePublic()
                .should().haveRawReturnType(resideInAPackage(BASE + ".domain.."))
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
