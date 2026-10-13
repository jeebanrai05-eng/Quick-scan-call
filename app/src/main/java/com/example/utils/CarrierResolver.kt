package com.example.utils

import androidx.compose.ui.graphics.Color

data class CarrierOrigin(
    val country: String,
    val region: String,
    val carrier: String,
    val carrierCode: String, // "NTC", "NCELL", "SMART", "JIO", "AIRTEL", "US", "GLOBAL"
    val brandColor: Color
)

object CarrierResolver {

    val NtcBlue = Color(0xFF0072CE)
    val NcellPurple = Color(0xFF6B1D78)
    val SmartOrange = Color(0xFFFF6600)
    val JioCyan = Color(0xFF0A66C2)
    val AirtelRed = Color(0xFFE40000)
    val InternationalCyan = Color(0xFF00E5FF)
    val GenericGreen = Color(0xFF00FF66)

    fun resolve(phoneNumber: String): CarrierOrigin {
        val clean = phoneNumber.trim().replace(" ", "").replace("-", "")
        val digitsOnly = clean.filter { it.isDigit() }

        // 1. Nepal Telecom & Ncell Detection (+977 or local 10-digit)
        if (clean.startsWith("+977") || (clean.length == 10 && (clean.startsWith("98") || clean.startsWith("97")))) {
            val localNumber = if (clean.startsWith("+977")) clean.removePrefix("+977") else clean

            return when {
                // NTC Mobile (984, 985, 986, 974, 975)
                localNumber.startsWith("984") || localNumber.startsWith("985") || localNumber.startsWith("986") -> {
                    CarrierOrigin("Nepal", "NTC GSM Mobile", "Nepal Telecom (Namaste)", "NTC", NtcBlue)
                }
                localNumber.startsWith("974") || localNumber.startsWith("975") || localNumber.startsWith("976") -> {
                    CarrierOrigin("Nepal", "NTC 4G/CDMA Mobile", "Nepal Telecom", "NTC", NtcBlue)
                }
                // Ncell Mobile (980, 981, 982)
                localNumber.startsWith("980") || localNumber.startsWith("981") || localNumber.startsWith("982") -> {
                    CarrierOrigin("Nepal", "Ncell 4G/5G Network", "Ncell Axiata", "NCELL", NcellPurple)
                }
                // Smart Cell (988, 972)
                localNumber.startsWith("988") || localNumber.startsWith("972") -> {
                    CarrierOrigin("Nepal", "Smart Cell Network", "Smart Telecom", "SMART", SmartOrange)
                }
                // Landlines
                localNumber.startsWith("01") || (localNumber.length == 8 && localNumber.startsWith("1")) -> {
                    CarrierOrigin("Nepal", "Kathmandu Valley", "NTC Landline (PSTN)", "LANDLINE", NtcBlue)
                }
                localNumber.startsWith("061") -> {
                    CarrierOrigin("Nepal", "Pokhara (Gandaki)", "NTC Landline", "LANDLINE", NtcBlue)
                }
                localNumber.startsWith("021") -> {
                    CarrierOrigin("Nepal", "Biratnagar (Koshi)", "NTC Landline", "LANDLINE", NtcBlue)
                }
                localNumber.startsWith("025") -> {
                    CarrierOrigin("Nepal", "Dharan (Sunsari)", "NTC Landline", "LANDLINE", NtcBlue)
                }
                localNumber.startsWith("056") -> {
                    CarrierOrigin("Nepal", "Bharatpur (Chitwan)", "NTC Landline", "LANDLINE", NtcBlue)
                }
                localNumber.startsWith("071") -> {
                    CarrierOrigin("Nepal", "Butwal (Lumbini)", "NTC Landline", "LANDLINE", NtcBlue)
                }
                localNumber.startsWith("081") -> {
                    CarrierOrigin("Nepal", "Nepalgunj (Banke)", "NTC Landline", "LANDLINE", NtcBlue)
                }
                localNumber.startsWith("091") -> {
                    CarrierOrigin("Nepal", "Dhangadhi (Kailali)", "NTC Landline", "LANDLINE", NtcBlue)
                }
                else -> {
                    CarrierOrigin("Nepal", "Nepal Cellular", "Nepal Telecom / Ncell", "NP", GenericGreen)
                }
            }
        }

        // 2. India (+91 or 10-digit starting with 6/7/8/9 if marked +91)
        if (clean.startsWith("+91")) {
            val localNumber = clean.removePrefix("+91")
            return when {
                localNumber.startsWith("6") || localNumber.startsWith("70") -> {
                    CarrierOrigin("India", "Jio Telecom Circle", "Reliance Jio 5G", "JIO", JioCyan)
                }
                localNumber.startsWith("98") || localNumber.startsWith("99") -> {
                    CarrierOrigin("India", "Airtel Circle", "Bharti Airtel", "AIRTEL", AirtelRed)
                }
                localNumber.startsWith("91") || localNumber.startsWith("92") || localNumber.startsWith("93") -> {
                    CarrierOrigin("India", "Vi Network", "Vodafone Idea", "VI", Color(0xFFFFCC00))
                }
                else -> {
                    CarrierOrigin("India", "National Cellular", "Jio / Airtel / Vi", "IN", GenericGreen)
                }
            }
        }

        // 3. USA / Canada (+1)
        if (clean.startsWith("+1") || (clean.length == 10 && !clean.startsWith("+") && !clean.startsWith("9"))) {
            return CarrierOrigin("US / CA", "North America", "Cellular / VoIP Network", "US", InternationalCyan)
        }

        // 4. Middle East & Gulf (Very popular for diaspora calls)
        if (clean.startsWith("+971")) {
            return CarrierOrigin("UAE", "Dubai / Abu Dhabi", "e& (Etisalat) / du", "UAE", Color(0xFF00C853))
        }
        if (clean.startsWith("+966")) {
            return CarrierOrigin("Saudi Arabia", "Riyadh / Jeddah", "STC / Mobily / Zain", "KSA", Color(0xFF00897B))
        }
        if (clean.startsWith("+974")) {
            return CarrierOrigin("Qatar", "Doha", "Ooredoo / Vodafone Qatar", "QATAR", Color(0xFF880E4F))
        }
        if (clean.startsWith("+965")) {
            return CarrierOrigin("Kuwait", "Kuwait City", "Zain / Ooredoo / STC", "KUWAIT", Color(0xFF1E88E5))
        }
        if (clean.startsWith("+60")) {
            return CarrierOrigin("Malaysia", "Kuala Lumpur", "Maxis / CelcomDigi", "MALAYSIA", Color(0xFFFB8C00))
        }

        // 5. UK, Europe, Australia, Japan
        if (clean.startsWith("+44")) {
            return CarrierOrigin("UK", "United Kingdom", "EE / O2 / Vodafone UK", "UK", InternationalCyan)
        }
        if (clean.startsWith("+61")) {
            return CarrierOrigin("Australia", "Sydney / Melbourne", "Telstra / Optus", "AU", InternationalCyan)
        }
        if (clean.startsWith("+81")) {
            return CarrierOrigin("Japan", "Tokyo / Osaka", "NTT Docomo / SoftBank", "JP", InternationalCyan)
        }
        if (clean.startsWith("+82")) {
            return CarrierOrigin("South Korea", "Seoul", "SK Telecom / KT", "KR", InternationalCyan)
        }
        if (clean.startsWith("+49")) {
            return CarrierOrigin("Germany", "Berlin", "Telekom / Vodafone", "DE", InternationalCyan)
        }

        // Generic International
        if (clean.startsWith("+")) {
            return CarrierOrigin("International", "Overseas Network", "Global Carrier", "GLOBAL", InternationalCyan)
        }

        return CarrierOrigin("Local", "Mobile", "Cellular Network", "LOCAL", GenericGreen)
    }
}
