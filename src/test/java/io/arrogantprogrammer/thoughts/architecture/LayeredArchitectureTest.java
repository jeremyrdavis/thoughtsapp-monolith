package io.arrogantprogrammer.thoughts.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

class LayeredArchitectureTest {

    private static final String BASE = "io.arrogantprogrammer.thoughts";

    private static final JavaClasses CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages(BASE);

    @Test
    void domainDependsOnNothingButJdk() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE + ".domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        BASE + ".application..",
                        BASE + ".adapters..");
        rule.check(CLASSES);
    }

    @Test
    void applicationDoesNotDependOnAdapters() {
        ArchRule rule = noClasses().that().resideInAPackage(BASE + ".application..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".adapters..");
        rule.check(CLASSES);
    }

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

    @Test
    void noAdapterTypeIsReferencedFromDomainOrApplication() {
        ArchRule rule = noClasses().that().resideInAnyPackage(
                        BASE + ".domain..",
                        BASE + ".application..")
                .should().dependOnClassesThat().resideInAPackage(BASE + ".adapters..");
        rule.check(CLASSES);
    }

    @Test
    void applicationServiceMethodsDoNotReturnDomainTypes() {
        ArchRule rule = noMethods().that().areDeclaredInClassesThat().resideInAPackage(BASE + ".application..")
                .and().arePublic()
                .should().haveRawReturnType(resideInAPackage(BASE + ".domain.."))
                .allowEmptyShould(true);
        rule.check(CLASSES);
    }
}
