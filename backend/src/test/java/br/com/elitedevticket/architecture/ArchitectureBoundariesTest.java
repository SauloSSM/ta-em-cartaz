package br.com.elitedevticket.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(
        packages = "br.com.elitedevticket",
        importOptions = {ImportOption.DoNotIncludeTests.class})
class ArchitectureBoundariesTest {

    private static final String[] APPROVED_FEATURE_MODULES = {
        "auth", "catalog", "events", "reservations", "payments", "tickets", "gate"
    };

    @ArchTest
    static final ArchRule controllersMustNotDependOnRepositoriesDirectly = noClasses()
            .that().resideInAPackage("..http..")
            .or().areAnnotatedWith(RestController.class)
            .or().areAnnotatedWith(Controller.class)
            .should().dependOnClassesThat().resideInAPackage("..adapters.persistence..")
            .orShould().dependOnClassesThat().areAnnotatedWith(Repository.class)
            .orShould().dependOnClassesThat().areAssignableTo(org.springframework.data.repository.Repository.class)
            .because("Controllers must delegate to application services and never access persistence repositories directly (AD-1, AD-21)");

    @ArchTest
    static final ArchRule httpLayerMustNotDependOnJpaEntities = noClasses()
            .that().resideInAPackage("..http..")
            .should().dependOnClassesThat().areAnnotatedWith(Entity.class)
            .orShould().dependOnClassesThat().resideInAPackage("..adapters.persistence..")
            .because("HTTP layer and DTOs must never expose or depend directly on JPA entities or persistence adapters (AD-12, AD-21)");

    @ArchTest
    static final ArchRule jpaEntitiesMustResideInPersistenceAdapters = classes()
            .that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..adapters.persistence..")
            .because("JPA entities must reside exclusively in persistence adapter packages (AD-1)");

    @ArchTest
    void moduleAdaptersMustRemainInternalToTheirModule(JavaClasses importedClasses) {
        for (String module : APPROVED_FEATURE_MODULES) {
            ArchRule rule = classes()
                    .that().resideInAPackage("..elitedevticket." + module + ".adapters..")
                    .should().onlyBeAccessed().byAnyPackage("..elitedevticket." + module + "..", "br.com.elitedevticket")
                    .because("Adapters of " + module + " are module-internal and must not be accessed from other modules (AD-1, AD-21)")
                    .allowEmptyShould(true);
            rule.check(importedClasses);
        }
    }

    @ArchTest
    static final ArchRule sharedMustNotDependOnFeatureModules = noClasses()
            .that().resideInAPackage("..shared..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..auth..",
                    "..catalog..",
                    "..events..",
                    "..reservations..",
                    "..payments..",
                    "..tickets..",
                    "..gate..")
            .because("Shared package must provide cross-cutting infrastructure without depending on feature modules (AD-1)");

    @ArchTest
    static final ArchRule noCeremonialImplOrGenericHelperNames = noClasses()
            .should().haveSimpleNameEndingWith("Impl")
            .orShould().haveSimpleNameEndingWith("Helper")
            .orShould().haveSimpleNameEndingWith("Util")
            .orShould().haveSimpleNameEndingWith("Utils")
            .orShould().haveSimpleNameEndingWith("Manager")
            .because("Ceremonial Impl classes and generic Helper/Util/Manager names are prohibited by engineering standards");

    @ArchTest
    static final ArchRule noFieldInjection = noFields()
            .should().beAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
            .orShould().beAnnotatedWith("jakarta.inject.Inject")
            .because("Field injection is prohibited; constructor injection must be used (Java Engineering Standards)");

    @ArchTest
    static final ArchRule applicationAndDomainMustNotDependOnHttpLayer = noClasses()
            .that().resideInAPackage("..application..")
            .or().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAPackage("..http..")
            .orShould().dependOnClassesThat().resideInAPackage("org.springframework.web..")
            .because("Application and domain layers must remain decoupled from HTTP/web contracts (AD-1)");

    @ArchTest
    static final ArchRule productionCodeMustNotUseUninjectedTime = noClasses()
            .that().resideInAPackage("br.com.elitedevticket..")
            .should().callMethod(Instant.class, "now")
            .orShould().callMethod(System.class, "currentTimeMillis")
            .because("Time operations must use injected Clock for deterministic execution and testability (AD-17, Java Standards)");

    @Test
    void testsMustNotDependOnThreadSleep() {
        JavaClasses testClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.ONLY_INCLUDE_TESTS)
                .importPackages("br.com.elitedevticket");

        ArchRule rule = noClasses()
                .should().callMethod(Thread.class, "sleep", long.class)
                .orShould().callMethod(Thread.class, "sleep", long.class, int.class)
                .because("Tests must be deterministic and must never rely on Thread.sleep (Java Engineering Standards)");

        rule.check(testClasses);
    }
}
