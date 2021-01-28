@ExperimentalUnsignedTypes
fun UShort.getLeft(): UByte {
    return (this.toUInt() shr 8).toUByte()
}

@ExperimentalUnsignedTypes
fun UShort.getRight(): UByte {
    return this.toUByte()
}