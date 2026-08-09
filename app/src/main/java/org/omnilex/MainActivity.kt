package org.omnilex

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import org.omnilex.data.local.DatabaseProvider
import org.omnilex.data.repository.LexicalRepository
import org.omnilex.ui.OmniLexApp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val database = DatabaseProvider.getDatabase(applicationContext)

        val repository = LexicalRepository(
            database.omniLexDao()
        )

        setContent {
            OmniLexApp(repository)
        }
    }
}