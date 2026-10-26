package com.example.sample.family.membersListingScreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sample.family.data.FamilyMemberEntity


@Composable
fun MemberFormContent(
    initial: FamilyMemberEntity?,
    onSave: (name: String, relationship: String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(initial) { mutableStateOf(initial?.name ?: "") }
    var relationship by remember(initial) { mutableStateOf(initial?.relationship ?: "") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var relError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (initial == null) "Add Family Member" else "Edit Family Member",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Spacer(Modifier.height(4.dp))

        OutlinedTextField(
            value = name,
            onValueChange = {
                name = it
                nameError = null
            },
            label = { Text("Name *") },
            placeholder = { Text("e.g. John Doe") },
            modifier = Modifier.fillMaxWidth(),
            isError = nameError != null,
            supportingText = {
                nameError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            singleLine = true,
        )

        OutlinedTextField(
            value = relationship,
            onValueChange = {
                relationship = it
                relError = null
            },
            label = { Text("Relationship *") },
            placeholder = { Text("e.g. Father, Spouse, Son…") },
            modifier = Modifier.fillMaxWidth(),
            isError = relError != null,
            supportingText = {
                relError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            singleLine = true,
        )

        Spacer(Modifier.height(4.dp))

        Button(
            onClick = {
                var valid = true
                if (name.isBlank()) {
                    nameError = "Name is required"
                    valid = false
                }
                if (relationship.isBlank()) {
                    relError = "Relationship is required"
                    valid = false
                }
                if (valid) onSave(name, relationship)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Text(
                text = if (initial == null) "Add Member" else "Save Changes",
                style = MaterialTheme.typography.labelLarge,
            )
        }

        TextButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Cancel")
        }

        Spacer(Modifier.height(8.dp))
    }
}
