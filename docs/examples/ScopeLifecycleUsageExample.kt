package io.github.wykopmobilny.docs.examples

import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsKey
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope
import io.github.wykopmobilny.kotlin.AppScopes

/**
 * Przykłady poprawnego użycia mechanizmu scope lifecycle.
 *
 * PROBLEM Z TASK #265:
 * Scope był utworzony z kluczem, ale próba użycia bez klucza powodowała błąd.
 */

class ScopeLifecycleUsageExample(
    private val appScopes: AppScopes,
    private val key: LinkDetailsKey,
) {
    /**
     * ❌ BŁĄD - Tak jak było w Task #265
     *
     * Scope został utworzony z kluczem LinkDetailsKey, ale próbujemy użyć
     * metody `safe` która nie przekazuje klucza (id = null).
     *
     * Rezultat:
     * - Klucze nie pasują: "...LinkDetailsScope=LinkDetailsKey(...)" vs "...LinkDetailsScope=null"
     * - Operacja NIE zostanie wykonana
     * - W logach pojawi się warning: "launchScoped didn't find scope for key=..."
     */
    fun wrongUsage_WillNotExecute() {
        // To NIE ZADZIAŁA!
        appScopes.safe<LinkDetailsScope> {
            // Ten kod NIGDY się nie wykona bo scope nie zostanie znaleziony
            println("This will never print!")
        }
    }

    /**
     * ✅ POPRAWNE - Tak jak naprawiono w Task #265
     *
     * Używamy metody `safeKeyed` która przekazuje ten sam klucz co był użyty
     * przy tworzeniu scope.
     *
     * Rezultat:
     * - Klucze pasują: "...LinkDetailsScope=LinkDetailsKey(...)" == "...LinkDetailsScope=LinkDetailsKey(...)"
     * - Operacja zostanie wykonana
     * - Kod wewnątrz bloku wykona się w coroutine scope powiązanym ze scope
     */
    fun correctUsage_WillExecute() {
        // To ZADZIAŁA!
        appScopes.safeKeyed<LinkDetailsScope>(id = key) {
            // Ten kod się wykona bo scope zostanie znaleziony
            println("This will execute correctly!")
        }
    }

    /**
     * Zasada ogólna:
     *
     * Jeśli scope jest tworzony Z KLUCZEM (scopeId != null):
     * - ZAWSZE używaj `safeKeyed<T>(id = key)` lub `launchInKeyed<T>(id = key)`
     * - NIGDY nie używaj `safe<T>()` ani `launchIn<T>()`
     *
     * Jeśli scope jest tworzony BEZ KLUCZA (scopeId = null):
     * - Możesz używać `safe<T>()` lub `launchIn<T>()`
     * - Ale lepiej jest zawsze używać klucza dla jednoznaczności
     */
}

/**
 * Przykład rzeczywistego użycia w GetRelatedLinksQuery.kt
 */
class GetRelatedLinksQueryExample(
    private val appScopes: AppScopes,
    private val key: LinkDetailsKey,
) {
    /**
     * PRZED naprawą (Task #265):
     */
    fun refresh_BEFORE() {
        appScopes.safe<LinkDetailsScope> {  // ❌ Bez klucza!
            // Operacja nie zadziała bo klucz nie pasuje
        }
    }

    /**
     * PO naprawie (Task #265):
     */
    fun refresh_AFTER() {
        appScopes.safeKeyed<LinkDetailsScope>(id = key) {  // ✅ Z kluczem!
            // Operacja zadziała poprawnie
        }
    }
}

/**
 * Przykład testowania czy scope działa poprawnie:
 */
class ScopeTestingExample(
    private val appScopes: AppScopes,
) {
    fun testScope() {
        val key = LinkDetailsKey(linkId = 123, initialCommentId = null)

        // Test 1: Operacja z odpowiednim kluczem powinna działać
        var executed = false
        appScopes.safeKeyed<LinkDetailsScope>(id = key) {
            executed = true
        }
        assert(executed) { "Operation with matching key should execute" }

        // Test 2: Operacja bez klucza (gdy scope ma klucz) NIE powinna działać
        var executedWrong = false
        appScopes.safe<LinkDetailsScope> {
            executedWrong = true
        }
        assert(!executedWrong) { "Operation without key should NOT execute when scope has key" }
    }
}

/**
 * Mechanizm wewnętrzny (dla zrozumienia):
 *
 * 1. Tworzenie scope:
 *    WykopApp.getDependency(LinkDetailsComponent::class, key = LinkDetailsKey(linkId=123))
 *    → Klucz: "io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope=LinkDetailsKey(linkId=123, initialCommentId=null)"
 *    → Scope zostaje dodany do mapy: scopes[klucz] = SubScope(...)
 *
 * 2a. Użycie z kluczem (POPRAWNE):
 *     appScopes.safeKeyed<LinkDetailsScope>(id = LinkDetailsKey(linkId=123))
 *     → Klucz: "io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope=LinkDetailsKey(linkId=123, initialCommentId=null)"
 *     → scopes[klucz] ISTNIEJE → operacja wykonana ✅
 *
 * 2b. Użycie bez klucza (BŁĘDNE):
 *     appScopes.safe<LinkDetailsScope>()
 *     → Klucz: "io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope=null"
 *     → scopes[klucz] NIE ISTNIEJE → warning w logach, operacja pominięta ❌
 *
 * 3. Niszczenie scope:
 *    WykopApp.destroyDependency(LinkDetailsComponent::class, key = LinkDetailsKey(linkId=123))
 *    → Scope zostaje usunięty z mapy: scopes.remove(klucz)
 *    → Coroutine scope zostaje anulowany: coroutineScope.cancel()
 */
