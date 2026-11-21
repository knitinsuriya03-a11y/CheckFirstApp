package com.example.domain

import java.text.Normalizer
import java.util.Locale

/**
 * Normalized representations of user input text for multilingual scam detection.
 * [original] is always preserved verbatim for UI display.
 */
data class NormalizedMessage(
    val original: String,
    val normalized: String,
    val deobfuscated: String,
    val clauses: List<String>,
    val wordCount: Int,
    val meaningfulCharCount: Int,
)

/**
 * Multilingual text normalizer supporting English, Tamil script, Tanglish,
 * Unicode homoglyphs, zero-width characters, unusual whitespace, and common
 * leetspeak / spaced-letter obfuscations.
 *
 * Informal Tamil or code-mixing is normalized neutrally and is NEVER penalized.
 */
object TextNormalizer {

    private val invisibleCharsRegex = Regex("""[\u200B-\u200F\u202A-\u202E\u2060-\u206F\uFEFF\u00AD]""")
    private val unicodeWhitespaceRegex = Regex("""[\u00A0\u1680\u2000-\u200A\u2028\u2029\u202F\u205F\u3000\t\r]+""")
    private val multiSpaceRegex = Regex("""[ ]{2,}""")
    private val clauseSplitRegex = Regex("""(?:[.!?\n;।]+|\s+(?:but|however|also|otherwise|or else|ஆனால்|இல்லையென்றால்|இல்லனா|illana|aana|apram)\s+)""", RegexOption.IGNORE_CASE)

    // Common Cyrillic & Greek visual homoglyphs used to evade keyword filters
    private val homoglyphMap: Map<Char, Char> = mapOf(
        'а' to 'a', 'А' to 'A',
        'е' to 'e', 'Е' to 'E',
        'о' to 'o', 'О' to 'O',
        'р' to 'p', 'Р' to 'P',
        'с' to 'c', 'С' to 'C',
        'х' to 'x', 'Х' to 'X',
        'і' to 'i', 'І' to 'I',
        'у' to 'y', 'У' to 'Y',
        'к' to 'k', 'К' to 'K',
        'м' to 'm', 'М' to 'M',
        'т' to 't', 'Т' to 'T',
        'ν' to 'v', 'α' to 'a', 'ο' to 'o', 'ρ' to 'p',
    )

    // Spaced/punctuated sensitive acronyms and terms (e.g., "O T P", "O.T.P", "P 1 N", "U P I", "A.P.K")
    private val spacedObfuscations: List<Pair<Regex, String>> = listOf(
        Regex("""\b[o0][\s._-]+[t7][\s._-]+[p]\b""", RegexOption.IGNORE_CASE) to "otp",
        Regex("""\b[p][\s._-]+[i1!|][\s._-]+[n]\b""", RegexOption.IGNORE_CASE) to "pin",
        Regex("""\b[u][\s._-]+[p][\s._-]+[i1!|]\b""", RegexOption.IGNORE_CASE) to "upi",
        Regex("""\b[c][\s._-]+[v][\s._-]+[v]\b""", RegexOption.IGNORE_CASE) to "cvv",
        Regex("""\b[k][\s._-]+[y][\s._-]+[c]\b""", RegexOption.IGNORE_CASE) to "kyc",
        Regex("""\b[a@][\s._-]+[p][\s._-]+[k]\b""", RegexOption.IGNORE_CASE) to "apk",
        Regex("""\b[c][\s._-]+[b][\s._-]+[i1!|]\b""", RegexOption.IGNORE_CASE) to "cbi",
        Regex("""\b[r][\s._-]+[b][\s._-]+[i1!|]\b""", RegexOption.IGNORE_CASE) to "rbi",
        Regex("""\b[a@][\s._-]*n[\s._-]*y[\s._-]*d[\s._-]*[e3][\s._-]*[s5$][\s._-]*k\b""", RegexOption.IGNORE_CASE) to "anydesk",
        Regex("""\bt[\s._-]*[e3][\s._-]*[a@][\s._-]*m[\s._-]*v[\s._-]*[i1][\s._-]*[e3][\s._-]*w[\s._-]*[e3][\s._-]*r\b""", RegexOption.IGNORE_CASE) to "teamviewer",
    )

    // Common leetspeak substitutions inside alphabetic words (avoids corrupting pure numbers like 482913)
    private val mixedWordRegex = Regex("""\b(?=[a-zA-Z0-9@$!]*[a-zA-Z])(?=[a-zA-Z0-9@$!]*[013457@$!])[a-zA-Z0-9@$!]{3,}\b""")

    private val commonMisspellings: List<Pair<Regex, String>> = listOf(
        Regex("""\bsh[a@4]r[e3]\b""", RegexOption.IGNORE_CASE) to "share",
        Regex("""\bp[a@4][s5$]{1,2}w[o0]rd\b""", RegexOption.IGNORE_CASE) to "password",
        Regex("""\br[e3]f[u]nd\b""", RegexOption.IGNORE_CASE) to "refund",
        Regex("""\bb[a@4]nk\b""", RegexOption.IGNORE_CASE) to "bank",
        Regex("""\b[a@4]cc[o0]unt\b""", RegexOption.IGNORE_CASE) to "account",
        Regex("""\bunbl[o0]ck\b""", RegexOption.IGNORE_CASE) to "unblock",
        Regex("""\bbl[o0]ck(ed)?\b""", RegexOption.IGNORE_CASE) to "blocked",
        Regex("""\bv[e3]r[i1]fy\b""", RegexOption.IGNORE_CASE) to "verify",
        Regex("""\bup[i1]\s*p[i1]n\b""", RegexOption.IGNORE_CASE) to "upi pin",
        Regex("""\b0tp\b""", RegexOption.IGNORE_CASE) to "otp",
        Regex("""\bp1n\b""", RegexOption.IGNORE_CASE) to "pin",
        Regex("""\bm-?p[i1]n\b""", RegexOption.IGNORE_CASE) to "mpin",
        Regex("""\b[u]rg[e3]nt\b""", RegexOption.IGNORE_CASE) to "urgent",
        Regex("""\b[i1]mm[e3]d[i1][a@4]t[e3]ly\b""", RegexOption.IGNORE_CASE) to "immediately",
        Regex("""\b[a@4]rr[e3][s5$]t\b""", RegexOption.IGNORE_CASE) to "arrest",
        Regex("""\bp[o0]l[i1]c[e3]\b""", RegexOption.IGNORE_CASE) to "police",
        Regex("""\bc[a@4][s5$]hb[a@4]ck\b""", RegexOption.IGNORE_CASE) to "cashback",
        Regex("""\btr[a@4]n[s5$]f[e3]r\b""", RegexOption.IGNORE_CASE) to "transfer",
        Regex("""\b[i1]n[s5$]t[a@4]ll\b""", RegexOption.IGNORE_CASE) to "install",
        Regex("""\bd[o0]wnl[o0][a@4]d\b""", RegexOption.IGNORE_CASE) to "download",
    )

    fun normalize(input: String): NormalizedMessage {
        val original = input
        if (input.isBlank()) {
            return NormalizedMessage(
                original = original,
                normalized = "",
                deobfuscated = "",
                clauses = emptyList(),
                wordCount = 0,
                meaningfulCharCount = 0,
            )
        }

        val stripped = invisibleCharsRegex.replace(input, "")
        val nfkc = Normalizer.normalize(stripped, Normalizer.Form.NFKC)
        val mapped = buildString(nfkc.length) {
            for (ch in nfkc) {
                append(homoglyphMap[ch] ?: ch)
            }
        }
        val cleanSpaces = multiSpaceRegex.replace(
            unicodeWhitespaceRegex.replace(mapped, " "),
            " ",
        ).trim()

        var deobf = cleanSpaces
        for ((pattern, replacement) in spacedObfuscations) {
            deobf = pattern.replace(deobf, replacement)
        }
        deobf = mixedWordRegex.replace(deobf) { m ->
            val token = m.value
            // Skip tokens that look like URLs/hosts or order IDs like "Order4421" / "INR500"
            if (token.startsWith("http", ignoreCase = true) || token.any { it == '.' }) {
                token
            } else {
                val lower = token.lowercase(Locale.ROOT)
                val converted = lower
                    .replace('0', 'o')
                    .replace('1', 'i')
                    .replace('3', 'e')
                    .replace('@', 'a')
                    .replace('$', 's')
                    .replace('!', 'i')
                // Only keep conversion if it matches a known security keyword or was clearly leetspeak
                if (converted != lower && isKnownDeobfWord(converted)) converted else token
            }
        }
        for ((pattern, replacement) in commonMisspellings) {
            deobf = pattern.replace(deobf, replacement)
        }

        val clauses = clauseSplitRegex.split(deobf)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val words = cleanSpaces
            .split(Regex("""[\s,.;:!?()\[\]{}"']+"""))
            .filter { token -> token.any { it.isLetterOrDigit() } }

        val meaningfulChars = cleanSpaces.count { it.isLetterOrDigit() }

        return NormalizedMessage(
            original = original,
            normalized = cleanSpaces,
            deobfuscated = deobf,
            clauses = clauses,
            wordCount = words.size,
            meaningfulCharCount = meaningfulChars,
        )
    }

    private fun isKnownDeobfWord(word: String): Boolean {
        return word in knownKeywords ||
            knownKeywords.any { word.contains(it) && word.length <= it.length + 3 }
    }

    private val knownKeywords = setOf(
        "otp", "pin", "upi", "cvv", "kyc", "apk", "cbi", "rbi",
        "share", "password", "refund", "bank", "account", "unblock",
        "block", "blocked", "verify", "urgent", "immediately", "arrest",
        "police", "cashback", "transfer", "install", "download", "anydesk",
        "teamviewer", "quicksupport", "warrant", "customs", "courier",
    )

    /**
     * Collects exact matched substrings for highlighting in the UI without mutating the original text.
     */
    fun findSpans(text: String, regex: Regex): List<String> {
        return regex.findAll(text)
            .map { it.value.trim() }
            .filter { it.length >= 2 }
            .distinct()
            .take(6)
            .toList()
    }
}
