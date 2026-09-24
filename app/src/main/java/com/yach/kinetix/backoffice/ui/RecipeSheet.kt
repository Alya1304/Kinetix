package com.yach.kinetix.backoffice.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yach.kinetix.backoffice.data.entity.IngredientEntity
import com.yach.kinetix.backoffice.data.entity.ProductEntity
import com.yach.kinetix.backoffice.data.entity.RecipeItemEntity
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeSheet(
    product: ProductEntity,
    recipeItems: List<RecipeItemEntity>,
    ingredients: List<IngredientEntity>,
    onDismiss: () -> Unit,
    onAddIngredient: (IngredientEntity, BigDecimal) -> Unit,
    onRemoveRecipeItem: (RecipeItemEntity) -> Unit
) {
    var selectedIngredient by remember { mutableStateOf<IngredientEntity?>(null) }
    var quantityStr by remember { mutableStateOf("") }
    var showDropdown by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.8f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text("Receta: ${product.name}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Form to add ingredient
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = showDropdown,
                    onExpandedChange = { showDropdown = !showDropdown },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedIngredient?.name ?: "Seleccionar insumo",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showDropdown) },
                        modifier = Modifier.menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        ingredients.forEach { ingredient ->
                            DropdownMenuItem(
                                text = { Text("${ingredient.name} (${ingredient.unitOfMeasure})") },
                                onClick = {
                                    selectedIngredient = ingredient
                                    showDropdown = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Cant.") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(80.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val q = quantityStr.toBigDecimalOrNull()
                    if (selectedIngredient != null && q != null && q > BigDecimal.ZERO) {
                        onAddIngredient(selectedIngredient!!, q)
                        selectedIngredient = null
                        quantityStr = ""
                    }
                },
                modifier = Modifier.align(Alignment.End),
                enabled = selectedIngredient != null && quantityStr.toBigDecimalOrNull() != null
            ) {
                Text("Agregar")
            }

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Insumos Actuales", style = MaterialTheme.typography.titleMedium)
            if (recipeItems.isEmpty()) {
                Text("No hay insumos en esta receta.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recipeItems) { item ->
                        val ing = ingredients.find { it.id == item.ingredientId }
                        ListItem(
                            headlineContent = { Text(ing?.name ?: "Insumo desconocido") },
                            supportingContent = { Text("Cantidad: ${item.requiredQuantity} ${ing?.unitOfMeasure ?: ""}") },
                            trailingContent = {
                                IconButton(onClick = { onRemoveRecipeItem(item) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
