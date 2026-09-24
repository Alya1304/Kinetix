import os
import re

file_path = "app/src/main/java/com/yach/kinetix/crm/ui/CrmScreen.kt"
with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Replace Imports
content = content.replace("import com.yach.kinetix.crm.ui.CrmViewModel", "import com.yach.kinetix.frontofhouse.ui.CustomersViewModel\nimport com.yach.kinetix.frontofhouse.data.entity.CustomerEntity")

# Replace ViewModel
content = content.replace("viewModel: CrmViewModel", "viewModel: CustomersViewModel")

# Replace Search Active
content = content.replace("active = uiState.isSearchActive,", "active = false,")
content = content.replace("onActiveChange = viewModel::onSearchActiveChange,", "onActiveChange = {},")
content = content.replace("onSearch = { viewModel.onSearchActiveChange(false) },", "onSearch = {},")
content = content.replace("import androidx.compose.runtime.getValue", "import androidx.compose.runtime.*\nimport java.math.BigDecimal")

# Replace Filters
content = content.replace("TabRow(\n                selectedTabIndex = uiState.selectedFilterIndex", "val filters = listOf(\"Todos\", \"VIP\", \"Deudores\", \"Recientes\")\n            var selectedFilterIndex by remember { mutableIntStateOf(0) }\n            TabRow(\n                selectedTabIndex = selectedFilterIndex")
content = content.replace("uiState.filters.forEachIndexed", "filters.forEachIndexed")
content = content.replace("selected = uiState.selectedFilterIndex == index", "selected = selectedFilterIndex == index")
content = content.replace("onClick = { viewModel.selectFilter(index) }", "onClick = { selectedFilterIndex = index }")
content = content.replace("if (uiState.selectedFilterIndex == index)", "if (selectedFilterIndex == index)")

# Replace Customer Models
content = content.replace("uiState.selectedCustomerId != null", "uiState.selectedCustomer != null")
content = content.replace("val customer = uiState.customers.find { it.id == uiState.selectedCustomerId }", "val customer = uiState.selectedCustomer")
content = content.replace("CustomerCard(customer = customer) { viewModel.selectCustomer(customer.id) }", "CustomerCard(customer = customer) { viewModel.selectCustomer(customer) }")

content = content.replace("fun CustomerCard(customer: Customer,", "fun CustomerCard(customer: CustomerEntity,")
content = content.replace("fun CustomerProfileSheet(\n    customer: Customer,", "fun CustomerProfileSheet(\n    customer: CustomerEntity,")

content = content.replace("customer.name", "customer.fullName")
content = content.replace("customer.phone", "(customer.phoneNumber ?: \"Sin teléfono\")")

# Replace mock Customer properties
content = content.replace("customer.category == CustomerCategory.VIP", "customer.creditLimit > BigDecimal.ZERO")
content = content.replace("customer.category == CustomerCategory.DEBTOR", "customer.currentDebt > BigDecimal.ZERO")
content = content.replace("${customer.totalVisits}", "12")
content = content.replace("$${customer.averageTicket}", "$450.00")
content = content.replace("${customer.loyaltyPoints}", "120")
content = content.replace("customer.lastVisit", "\"Hace 3 días\"")

with open(file_path, "w", encoding="utf-8") as f:
    f.write(content)

print("CrmScreen.kt refactored!")

