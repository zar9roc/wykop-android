package io.github.wykopmobilny.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.wykopmobilny.TestApp
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsComponent
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsKey
import io.github.wykopmobilny.domain.linkdetails.di.LinkDetailsScope
import io.github.wykopmobilny.kotlin.AppScopes
import io.github.wykopmobilny.kotlin.launchIn
import io.github.wykopmobilny.kotlin.launchInKeyed
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Testy integracyjne dla mechanizmu scope lifecycle.
 *
 * Celem jest wykrycie problemów podobnych do Task #265, gdzie użycie `safe<T>` zamiast
 * `safeKeyed<T>(id)` powodowało że operacje nie były wykonywane w odpowiednim scope.
 *
 * Dokumentacja: docs/SCOPE_LIFECYCLE_TESTS.md
 */
@RunWith(AndroidJUnit4::class)
class ScopeLifecycleTest {
    private lateinit var app: TestApp
    private val testKeys = mutableListOf<LinkDetailsKey>()

    @Before
    fun setup() {
        app = TestApp.instance
    }

    @After
    fun cleanup() {
        // Zniszcz wszystkie scope utworzone w testach
        testKeys.forEach { key ->
            runBlocking {
                try {
                    app.destroyDependency(LinkDetailsComponent::class, key)
                } catch (e: Exception) {
                    // Ignoruj błędy przy cleanup
                }
            }
        }
        testKeys.clear()
    }

    /**
     * Test 1: Sprawdza czy scope jest poprawnie utworzony z kluczem.
     */
    @Test
    fun testScopeCreationWithKey() {
        val key = LinkDetailsKey(linkId = 12345, initialCommentId = null)
        testKeys.add(key)

        // Utwórz scope
        val component = app.getDependency(LinkDetailsComponent::class, key)

        // Zweryfikuj że scope istnieje - możemy go pobrać ponownie
        val componentAgain = app.getDependency(LinkDetailsComponent::class, key)

        // Powinien być ten sam obiekt (singleton w ramach scope)
        assertTrue("Scope should be reused", component === componentAgain)
    }

    /**
     * Test 2: Sprawdza czy operacje wykonywane w scope z odpowiednim kluczem działają.
     */
    @Test
    fun testScopeUsageWithMatchingKey() {
        val key = LinkDetailsKey(linkId = 23456, initialCommentId = null)
        testKeys.add(key)

        // Utwórz scope
        app.getDependency(LinkDetailsComponent::class, key)

        val latch = CountDownLatch(1)
        val executed = AtomicBoolean(false)

        // Wykonaj operację w scope z ODPOWIEDNIM kluczem
        app.launchInKeyed<LinkDetailsScope>(id = key) {
            executed.set(true)
            latch.countDown()
        }

        // Poczekaj na wykonanie
        assertTrue(
            "Operation should complete within timeout",
            latch.await(5, TimeUnit.SECONDS),
        )
        assertTrue("Operation should be executed in matching scope", executed.get())
    }

    /**
     * Test 3: ⚠️ WYKRYWA BUG Z TASK #265
     *
     * Sprawdza że użycie `launchIn<T>` (bez klucza) gdy scope został utworzony
     * z kluczem NIE działa. To dokładnie symuluje bug z Task #265.
     */
    @Test
    fun testScopeUsageWithMismatchedKey_DetectsBug() {
        val key = LinkDetailsKey(linkId = 34567, initialCommentId = null)
        testKeys.add(key)

        // Utwórz scope Z KLUCZEM
        app.getDependency(LinkDetailsComponent::class, key)

        val latch = CountDownLatch(1)
        val executed = AtomicBoolean(false)

        // Spróbuj wykonać operację BEZ KLUCZA (tak jak był bug w Task #265)
        app.launchIn<LinkDetailsScope> {
            executed.set(true)
            latch.countDown()
        }

        // Poczekaj krócej (operacja nie powinna się wykonać)
        val completed = latch.await(2, TimeUnit.SECONDS)

        // Operacja NIE powinna się wykonać bo klucze nie pasują
        assertFalse(
            "Operation should NOT execute when key doesn't match (Bug from Task #265)",
            executed.get(),
        )
        assertFalse(
            "Latch should timeout because operation wasn't executed",
            completed,
        )

        // Warning "launchScoped didn't find scope" powinien pojawić się w logach
        // (nie możemy go tu zweryfikować bezpośrednio, ale można sprawdzić w logcat)
    }

    /**
     * Test 4: Sprawdza czy scope jest prawidłowo niszczony.
     */
    @Test
    fun testScopeDestruction() {
        val key = LinkDetailsKey(linkId = 45678, initialCommentId = null)
        testKeys.add(key)

        // Utwórz scope
        app.getDependency(LinkDetailsComponent::class, key)

        // Operacja przed zniszczeniem powinna działać
        val latchBefore = CountDownLatch(1)
        val executedBefore = AtomicBoolean(false)

        app.launchInKeyed<LinkDetailsScope>(id = key) {
            executedBefore.set(true)
            latchBefore.countDown()
        }

        assertTrue(latchBefore.await(5, TimeUnit.SECONDS))
        assertTrue("Operation before destroy should work", executedBefore.get())

        // Zniszcz scope
        app.destroyDependency(LinkDetailsComponent::class, key)

        // Operacja po zniszczeniu NIE powinna działać
        val latchAfter = CountDownLatch(1)
        val executedAfter = AtomicBoolean(false)

        app.launchInKeyed<LinkDetailsScope>(id = key) {
            executedAfter.set(true)
            latchAfter.countDown()
        }

        val completedAfter = latchAfter.await(2, TimeUnit.SECONDS)

        assertFalse(
            "Operation after destroy should NOT work",
            executedAfter.get(),
        )
        assertFalse(
            "Latch should timeout after scope destruction",
            completedAfter,
        )
    }

    /**
     * Test 5: Symuluje rzeczywisty scenariusz nawigacji który spowodował bug.
     *
     * Scenariusz: Link 1 → back → Link 2 → refresh
     */
    @Test
    fun testMultipleNavigationsScenario() {
        val key1 = LinkDetailsKey(linkId = 56789, initialCommentId = null)
        val key2 = LinkDetailsKey(linkId = 67890, initialCommentId = null)
        testKeys.addAll(listOf(key1, key2))

        // === Nawigacja 1: Link 1 ===
        app.getDependency(LinkDetailsComponent::class, key1)

        val latch1 = CountDownLatch(1)
        val counter = AtomicInteger(0)

        app.launchInKeyed<LinkDetailsScope>(id = key1) {
            counter.incrementAndGet()
            latch1.countDown()
        }

        assertTrue(latch1.await(5, TimeUnit.SECONDS))
        assertEquals("First navigation should execute", 1, counter.get())

        // Zniszcz scope 1 (symulacja back button)
        app.destroyDependency(LinkDetailsComponent::class, key1)

        // === Nawigacja 2: Link 2 ===
        app.getDependency(LinkDetailsComponent::class, key2)

        val latch2 = CountDownLatch(1)

        app.launchInKeyed<LinkDetailsScope>(id = key2) {
            counter.incrementAndGet()
            latch2.countDown()
        }

        assertTrue(latch2.await(5, TimeUnit.SECONDS))
        assertEquals("Second navigation should execute in NEW scope", 2, counter.get())

        // Próba użycia STAREGO scope - nie powinno działać
        val latchOld = CountDownLatch(1)
        val executedInOld = AtomicBoolean(false)

        app.launchInKeyed<LinkDetailsScope>(id = key1) {
            executedInOld.set(true)
            latchOld.countDown()
        }

        assertFalse(
            "Old scope should not work after destruction",
            latchOld.await(2, TimeUnit.SECONDS),
        )
        assertFalse(executedInOld.get())
    }

    /**
     * Test 6: Sprawdza czy coroutine scope jest anulowany przy niszczeniu.
     */
    @Test
    fun testScopeCoroutineCancellation() {
        val key = LinkDetailsKey(linkId = 78901, initialCommentId = null)
        testKeys.add(key)

        // Utwórz scope
        app.getDependency(LinkDetailsComponent::class, key)

        val latch = CountDownLatch(1)
        val completed = AtomicBoolean(false)
        val cancelled = AtomicBoolean(false)

        // Uruchom długą operację
        app.launchInKeyed<LinkDetailsScope>(id = key) {
            try {
                delay(10000) // 10 sekund
                completed.set(true)
            } catch (e: Exception) {
                // Operacja powinna być anulowana
                cancelled.set(true)
            } finally {
                latch.countDown()
            }
        }

        // Poczekaj chwilę aby operacja się rozpoczęła
        Thread.sleep(100)

        // Zniszcz scope (powinno anulować coroutine)
        app.destroyDependency(LinkDetailsComponent::class, key)

        // Poczekaj na zakończenie
        assertTrue(
            "Operation should complete (cancelled or finished)",
            latch.await(5, TimeUnit.SECONDS),
        )

        // Operacja powinna być anulowana, nie ukończona normalnie
        assertFalse(
            "Long operation should NOT complete normally after scope destruction",
            completed.get(),
        )
        assertTrue(
            "Operation should be cancelled when scope is destroyed",
            cancelled.get(),
        )
    }

    /**
     * Test 7: Sprawdza że różne scope z różnymi kluczami są niezależne.
     */
    @Test
    fun testIndependentScopes() {
        val key1 = LinkDetailsKey(linkId = 11111, initialCommentId = null)
        val key2 = LinkDetailsKey(linkId = 22222, initialCommentId = null)
        testKeys.addAll(listOf(key1, key2))

        // Utwórz dwa różne scope
        val component1 = app.getDependency(LinkDetailsComponent::class, key1)
        val component2 = app.getDependency(LinkDetailsComponent::class, key2)

        // Powinny być różnymi obiektami
        assertTrue(
            "Different keys should create different scopes",
            component1 !== component2,
        )

        // Operacje w obu scope powinny działać niezależnie
        val latch = CountDownLatch(2)
        val counter1 = AtomicInteger(0)
        val counter2 = AtomicInteger(0)

        app.launchInKeyed<LinkDetailsScope>(id = key1) {
            counter1.incrementAndGet()
            latch.countDown()
        }

        app.launchInKeyed<LinkDetailsScope>(id = key2) {
            counter2.incrementAndGet()
            latch.countDown()
        }

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertEquals("Scope 1 should execute", 1, counter1.get())
        assertEquals("Scope 2 should execute", 1, counter2.get())

        // Zniszczenie scope 1 nie powinno wpłynąć na scope 2
        app.destroyDependency(LinkDetailsComponent::class, key1)

        val latch2 = CountDownLatch(1)
        app.launchInKeyed<LinkDetailsScope>(id = key2) {
            counter2.incrementAndGet()
            latch2.countDown()
        }

        assertTrue(latch2.await(5, TimeUnit.SECONDS))
        assertEquals("Scope 2 should still work after destroying scope 1", 2, counter2.get())
    }
}
