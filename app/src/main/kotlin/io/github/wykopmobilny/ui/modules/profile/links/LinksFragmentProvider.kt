package io.github.wykopmobilny.ui.modules.profile.links

import dagger.Module
import dagger.android.ContributesAndroidInjector
import io.github.wykopmobilny.ui.modules.profile.links.added.ProfileLinksFragment
import io.github.wykopmobilny.ui.modules.profile.links.added.ProfileLinksModule

@Module
abstract class LinksFragmentProvider {
    @ContributesAndroidInjector(modules = [ProfileLinksModule::class])
    abstract fun provideAddedFragment(): ProfileLinksFragment

    // Komentarze i Powiazane uzywaja ActionsFragment (lista mieszana) - jego injector
    // jest juz w ProfileFragmentProvider i jest dziedziczony przez ten subkomponent.
}
