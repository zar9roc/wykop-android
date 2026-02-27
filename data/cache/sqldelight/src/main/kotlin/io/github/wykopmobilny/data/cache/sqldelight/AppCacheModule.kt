package io.github.wykopmobilny.data.cache.sqldelight

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.ColumnAdapter
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import dagger.Module
import dagger.Provides
import io.github.wykopmobilny.data.cache.api.AppCache
import io.github.wykopmobilny.data.cache.api.Embed
import io.github.wykopmobilny.data.cache.api.EmbedType
import io.github.wykopmobilny.data.cache.api.EntryEntity
import io.github.wykopmobilny.data.cache.api.GenderEntity
import io.github.wykopmobilny.data.cache.api.LinkCommentsEntity
import io.github.wykopmobilny.data.cache.api.LinkEntity
import io.github.wykopmobilny.data.cache.api.ProfileActionsEntity
import io.github.wykopmobilny.data.cache.api.ProfileEntity
import io.github.wykopmobilny.data.cache.api.RelatedLinkEntity
import io.github.wykopmobilny.data.cache.api.UserColorEntity
import io.github.wykopmobilny.data.cache.api.UserVote
import kotlinx.datetime.Instant
import javax.inject.Singleton

@Module
internal class AppCacheModule {
    @Singleton
    @Provides
    fun database(context: Context) =
        AppCache(
            driver =
                AndroidSqliteDriver(
                    schema = AppCache.Schema,
                    context = context,
                    name = null,
                    callback =
                        object : AndroidSqliteDriver.Callback(AppCache.Schema) {
                            override fun onOpen(db: SupportSQLiteDatabase) {
                                db.execSQL("PRAGMA foreign_keys=ON;")
                            }
                        },
                ),
            profileEntityAdapter =
                ProfileEntity.Adapter(
                    signupAtAdapter = InstantAdapter,
                    colorAdapter = EnumAdapter(UserColorEntity.entries),
                    genderAdapter = EnumAdapter(GenderEntity.entries),
                    linksAddedCountAdapter = IntAdapter,
                    linksPublishedCountAdapter = IntAdapter,
                    commentsCountAdapter = IntAdapter,
                    rankAdapter = IntAdapter,
                    followersAdapter = IntAdapter,
                    followingAdapter = IntAdapter,
                    entriesCountAdapter = IntAdapter,
                    entriesCommentsCountAdapter = IntAdapter,
                    diggsCountAdapter = IntAdapter,
                    buriesCountAdapter = IntAdapter,
                ),
            linkEntityAdapter =
                LinkEntity.Adapter(
                    postedAtAdapter = InstantAdapter,
                    userVoteAdapter = EnumAdapter(UserVote.entries),
                    voteCountAdapter = IntAdapter,
                    buryCountAdapter = IntAdapter,
                    commentsCountAdapter = IntAdapter,
                    relatedCountAdapter = IntAdapter,
                ),
            entryEntityAdapter =
                EntryEntity.Adapter(
                    postedAtAdapter = InstantAdapter,
                    userVoteAdapter = EnumAdapter(UserVote.entries),
                    voteCountAdapter = IntAdapter,
                    commentsCountAdapter = IntAdapter,
                ),
            embedAdapter =
                Embed.Adapter(
                    typeAdapter = EnumAdapter(EmbedType.entries),
                    ratioAdapter = FloatAdapter,
                ),
            linkCommentsEntityAdapter =
                LinkCommentsEntity.Adapter(
                    postedAtAdapter = InstantAdapter,
                    userVoteAdapter = EnumAdapter(UserVote.entries),
                    voteCountAdapter = IntAdapter,
                    voteCountPlusAdapter = IntAdapter,
                ),
            relatedLinkEntityAdapter =
                RelatedLinkEntity.Adapter(
                    userVoteAdapter = EnumAdapter(UserVote.entries),
                    voteCountAdapter = IntAdapter,
                    orderOnPageAdapter = IntAdapter,
                ),
            profileActionsEntityAdapter =
                ProfileActionsEntity.Adapter(
                    pageAdapter = IntAdapter,
                    orderOnPageAdapter = IntAdapter,
                ),
        )
}

internal object InstantAdapter : ColumnAdapter<Instant, Long> {
    override fun decode(databaseValue: Long) = Instant.fromEpochMilliseconds(databaseValue)

    override fun encode(value: Instant) = value.toEpochMilliseconds()
}

internal object IntAdapter : ColumnAdapter<Int, Long> {
    override fun decode(databaseValue: Long) = databaseValue.toInt()

    override fun encode(value: Int) = value.toLong()
}

internal object FloatAdapter : ColumnAdapter<Float, Double> {
    override fun decode(databaseValue: Double) = databaseValue.toFloat()

    override fun encode(value: Float) = value.toDouble()
}

class EnumAdapter<T : Enum<T>>(
    private val values: List<T>,
) : ColumnAdapter<T, Long> {
    override fun decode(databaseValue: Long) = values[databaseValue.toInt()]

    override fun encode(value: T) = value.ordinal.toLong()
}
