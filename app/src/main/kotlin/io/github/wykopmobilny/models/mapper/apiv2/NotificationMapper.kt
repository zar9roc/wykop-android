package io.github.wykopmobilny.models.mapper.apiv2

import io.github.wykopmobilny.api.responses.NotificationResponse
import io.github.wykopmobilny.models.dataclass.Notification
import io.github.wykopmobilny.models.mapper.Mapper

object NotificationMapper : Mapper<NotificationResponse, Notification> {
    override fun map(value: NotificationResponse) =
        Notification(
            // API v2 ma identyfikatory liczbowe - model trzyma je jako tekst.
            id = value.id.toString(),
            author = value.author?.let(AuthorMapper::map),
            body = value.body,
            date = value.date,
            type = value.type,
            url = value.url,
            new = value.new,
        )
}
