package io.github.wykopmobilny.models.mapper.apiv3

import io.github.wykopmobilny.api.responses.v3.notifications.NotificationEntryResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationPmResponseV3
import io.github.wykopmobilny.api.responses.v3.notifications.NotificationTagResponseV3
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.mapper.Mapper
import kotlinx.datetime.Instant

object NotificationEntryMapperV3 : Mapper<NotificationEntryResponseV3, Notification> {
    override fun map(value: NotificationEntryResponseV3) =
        Notification(
            id = value.id.toLongOrNull() ?: 0L,
            author = value.user?.let(AuthorMapperV3::map),
            body = value.message.orEmpty(),
            date = runCatching { Instant.parse(value.createdAt) }.getOrNull(),
            type = value.type,
            url = value.url,
            new = (value.read ?: 0) == 0,
        )
}

object NotificationPmMapperV3 : Mapper<NotificationPmResponseV3, Notification> {
    override fun map(value: NotificationPmResponseV3) =
        Notification(
            id = value.id.toLongOrNull() ?: 0L,
            author = value.user?.let(AuthorMapperV3::map),
            body = value.content.orEmpty(),
            date = runCatching { Instant.parse(value.createdAt) }.getOrNull(),
            type = value.type,
            url = null,
            new = (value.read ?: 0) == 0,
        )
}

object NotificationTagMapperV3 : Mapper<NotificationTagResponseV3, Notification> {
    override fun map(value: NotificationTagResponseV3) =
        Notification(
            id = value.id.toLongOrNull() ?: 0L,
            author = value.user?.let(AuthorMapperV3::map),
            body = value.tag?.name.orEmpty(),
            date = runCatching { Instant.parse(value.createdAt) }.getOrNull(),
            type = value.type,
            url = null,
            new = (value.read ?: 0) == 0,
        )
}
