package org.omnilex.data.repository

import org.omnilex.data.model.*

object SampleLexicon {
    val entries = listOf(
        LexicalEntry("bank-finance", "bank", "bank", "noun", "/bæŋk/", "b æ ŋ k", "B520", "en", "0",completeness = 72),
        LexicalEntry("bank-river", "bank", "bank river", "noun", "/bæŋk/", "b æ ŋ k", "B520", "en", "0", completeness = 68),
        LexicalEntry("financial-institution", "financial institution", "financial institution", "noun", null, null, "F524", "en", "0",completeness = 54),
        LexicalEntry("river", "river", "river", "noun", "/ˈrɪvər/", "r ɪ v ə r", "R160",  "en", "0" ,completeness = 82),
        LexicalEntry("shore", "shore", "shore", "noun", "/ʃɔːr/", "ʃ ɔː r", "S600",  "en", "0" ,completeness = 61),
        LexicalEntry("save", "save", "save", "verb", "/seɪv/", "s eɪ v", "S100",  "en", "0" ,completeness = 76)
    )
    val senses = listOf(
        Sense("s-bank-1", "bank-finance", "A financial institution that accepts deposits and lends money.", "An organization mediating money and credit.", "Used in finance, commerce, and personal banking.", null, "finance", 1),
        Sense("s-bank-2", "bank-river", "The sloping land beside a river or other body of water.", "A boundary formed by a body of water.", "Often occurs in geographic descriptions.", null, "geography", 1),
        Sense("s-river-1", "river", "A large natural stream of water flowing in a channel to the sea, a lake, or another river.", null, null, null, "geography", 1),
        Sense("s-save-1", "save", "Keep safe or reserve for future use.", null, "The financial sense commonly co-occurs with money.", null, "general", 1)
    )
    val relationships = listOf(
        LexicalRelationship("r-bank-homonym", "bank-finance", "bank-river", RelationshipType.HOMONYM, .99f, note = "Same spelling and pronunciation; distinct senses."),
        LexicalRelationship("r-bank-hyper", "bank-finance", "financial-institution", RelationshipType.HYPERNYM, .93f),
        LexicalRelationship("r-bank-syn", "bank-river", "shore", RelationshipType.SYNONYM, .66f),
        LexicalRelationship("r-bank-mer", "bank-river", "river", RelationshipType.RELATED_CONCEPT, .81f),
        LexicalRelationship("r-save-related", "save", "bank-finance", RelationshipType.RELATED_CONCEPT, .58f)
    )
}
