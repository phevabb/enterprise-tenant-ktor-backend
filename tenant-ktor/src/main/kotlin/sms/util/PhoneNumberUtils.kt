package sms.util

fun normalizeGhanaPhoneNumber(
    value: String?
): String? {
    var phoneNumber =
        value
            ?.trim()
            ?.replace(
                Regex("[\\s()-]"),
                ""
            )
            .orEmpty()

    if (phoneNumber.isBlank()) {
        return null
    }

    if (
        phoneNumber.startsWith("+")
    ) {
        phoneNumber =
            phoneNumber.drop(1)
    }

    if (
        phoneNumber.startsWith("00")
    ) {
        phoneNumber =
            phoneNumber.drop(2)
    }

    if (
        phoneNumber.startsWith("0") &&
        phoneNumber.length == 10
    ) {
        phoneNumber =
            "233" +
                    phoneNumber.drop(1)
    }

    if (
        !phoneNumber.matches(
            Regex("^233[0-9]{9}$")
        )
    ) {
        return null
    }

    return phoneNumber
}