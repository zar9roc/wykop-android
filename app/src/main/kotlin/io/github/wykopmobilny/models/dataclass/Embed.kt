package io.github.wykopmobilny.models.dataclass

import android.os.Parcel
import android.os.Parcelable

class Embed(
    val type: String,
    val preview: String,
    val url: String,
    val plus18: Boolean,
    val source: String?,
    val isAnimated: Boolean,
    val size: String,
    var isResize: Boolean = false,
    var isRevealed: Boolean = false,
    // Etykieta zdjecia z API (np. nazwa pliku od autora) - tytul w przegladarce i nazwa przy zapisie.
    val label: String? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
        parcel.readByte() != 0.toByte(),
        parcel.readString()!!,
        parcel.readByte() != 0.toByte(),
        parcel.readByte() != 0.toByte(),
        parcel.readString(),
    )

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeString(type)
        parcel.writeString(preview)
        parcel.writeString(url)
        parcel.writeByte(if (plus18) 1 else 0)
        parcel.writeString(source)
        parcel.writeByte(if (isAnimated) 1 else 0)
        parcel.writeString(size)
        parcel.writeByte(if (isResize) 1 else 0)
        parcel.writeByte(if (isRevealed) 1 else 0)
        parcel.writeString(label)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Embed> {
        override fun createFromParcel(parcel: Parcel): Embed = Embed(parcel)

        override fun newArray(size: Int): Array<Embed?> = arrayOfNulls(size)
    }
}
