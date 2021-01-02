@ExperimentalUnsignedTypes
fun UByte.concatToShort(second:UByte): UShort {
    return (this.toUInt() shl 8 or second.toUInt()).toUShort()
}

@ExperimentalUnsignedTypes
fun UShort.getLeft(): UByte {
    return (this.toUInt() shr 8).toUByte()
}

@ExperimentalUnsignedTypes
fun UShort.getRight(): UByte {
    return this.toUByte()
}