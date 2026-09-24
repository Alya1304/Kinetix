package com.yach.kinetix.core.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RoomService
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yach.kinetix.R
import com.yach.kinetix.backofhouse.ui.KdsScreen
import com.yach.kinetix.backofhouse.ui.KdsViewModel
import com.yach.kinetix.backoffice.ui.BackofficeScreen
import com.yach.kinetix.backoffice.ui.BackofficeViewModel
import com.yach.kinetix.frontofhouse.ui.CustomersScreen
import com.yach.kinetix.frontofhouse.ui.CustomersViewModel
import com.yach.kinetix.frontofhouse.ui.PosScreen
import com.yach.kinetix.frontofhouse.ui.PosViewModel
import com.yach.kinetix.frontofhouse.ui.SplitCheckScreen
import com.yach.kinetix.frontofhouse.ui.SplitCheckViewModel
import com.yach.kinetix.frontofhouse.ui.TablesScreen
import com.yach.kinetix.frontofhouse.ui.TablesViewModel
import com.yach.kinetix.treasury.ui.ExpensesScreen
import com.yach.kinetix.treasury.ui.ExpensesViewModel
import com.yach.kinetix.treasury.ui.TreasuryScreen
import com.yach.kinetix.treasury.ui.TreasuryViewModel

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object Login : Screen("login", "Login", Icons.Default.Person)
    data object Dashboard : Screen("dashboard", "Dashboard", Icons.Default.Dashboard)
    data object Pos : Screen("pos", "POS", Icons.Default.PointOfSale)
    data object Tables : Screen("tables", "Mesas", Icons.Default.TableRestaurant)
    data object Customers : Screen("customers", "Clientes", Icons.Default.People)
    data object SplitCheck : Screen("split_check", "Fraccionar", Icons.Default.RoomService)
    data object Kds : Screen("kds", "Cocina", Icons.Default.Restaurant)
    data object Treasury : Screen("treasury", "Caja", Icons.Default.Assessment)
    data object Expenses : Screen("expenses", "Gastos", Icons.Default.AttachMoney)
    data object Backoffice : Screen("backoffice", "Catálogo", Icons.Default.Inventory2)
    data object Settings : Screen("settings", "Ajustes", Icons.Default.Settings)
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val items = listOf(
        Screen.Dashboard,
        Screen.Pos,
        Screen.Tables,
        Screen.Customers,
        Screen.SplitCheck,
        Screen.Kds,
        Screen.Treasury,
        Screen.Expenses,
        Screen.Backoffice,
        Screen.Settings
    )

    val configuration = LocalConfiguration.current
    val isExpandedOrLandscape = configuration.screenWidthDp >= 600

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Login.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (currentRoute == Screen.Login.route) {
        AppNavHost(navController = navController)
        return
    }

    if (isExpandedOrLandscape) {
        // Modo Tablet / Terminal POS Horizontal
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            NavigationRail(
                modifier = Modifier
                    .width(96.dp)
                    .fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                header = {
                    Image(
                        painter = painterResource(id = R.drawable.kinetix_logo),
                        contentDescription = "Kinetix Logo",
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .size(56.dp)
                            .clip(RoundedCornerShape(14.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            ) {
                val scrollState = rememberScrollState()
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .verticalScroll(scrollState)
                ) {
                    items.forEach { screen ->
                        NavigationRailItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute == screen.route,
                            onClick = { navigateTo(screen.route) }
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    NavigationRailItem(
                        icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión") },
                        label = { Text("Salir", style = MaterialTheme.typography.labelSmall) },
                        selected = false,
                        onClick = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AppNavHost(navController = navController)
            }
        }
    } else {
        // Modo Mobile-First: ModalNavigationDrawer + BottomBar ergonómica para smartphones
        val drawerState = androidx.compose.material3.rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        androidx.compose.material3.ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                androidx.compose.material3.ModalDrawerSheet(
                    modifier = Modifier.width(300.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.kinetix_logo),
                            contentDescription = "Kinetix Logo",
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Kinetix POS",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Sistema de Restaurantes",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    val drawerScrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(drawerScrollState)
                            .padding(horizontal = 12.dp)
                    ) {
                        items.forEach { screen ->
                            NavigationDrawerItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title, fontWeight = FontWeight.Medium) },
                                selected = currentRoute == screen.route,
                                onClick = {
                                    coroutineScope.launch { drawerState.close() }
                                    navigateTo(screen.route)
                                },
                                modifier = Modifier.padding(vertical = 4.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        
                        NavigationDrawerItem(
                            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Cerrar Sesión") },
                            label = { Text("Cerrar Sesión", fontWeight = FontWeight.Medium) },
                            selected = false,
                            onClick = {
                                coroutineScope.launch { drawerState.close() }
                                navController.navigate(Screen.Login.route) {
                                    popUpTo(0)
                                }
                            },
                            modifier = Modifier.padding(vertical = 4.dp),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }
        ) {
            Scaffold(
                bottomBar = {
                    NavigationBar {
                        // Los 3 destinos más rápidos para meseros en teléfono + botón Más
                        val mobileQuickItems = listOf(Screen.Pos, Screen.Tables, Screen.Kds)
                        mobileQuickItems.forEach { screen ->
                            NavigationBarItem(
                                icon = { Icon(screen.icon, contentDescription = screen.title) },
                                label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                                selected = currentRoute == screen.route,
                                onClick = { navigateTo(screen.route) }
                            )
                        }
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Menu, contentDescription = "Más") },
                            label = { Text("Más", style = MaterialTheme.typography.labelSmall) },
                            selected = currentRoute !in mobileQuickItems.map { it.route },
                            onClick = {
                                coroutineScope.launch {
                                    if (drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    AppNavHost(navController = navController)
                }
            }
        }
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Login.route,
        modifier = modifier
    ) {
        composable(Screen.Login.route) {
            com.yach.kinetix.backoffice.ui.LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        composable(Screen.Dashboard.route) {
            val viewModel: com.yach.kinetix.dashboard.ui.DashboardViewModel = hiltViewModel()
            com.yach.kinetix.dashboard.ui.DashboardScreen(viewModel = viewModel)
        }
        composable(Screen.Pos.route) {
            val viewModel: PosViewModel = hiltViewModel()
            PosScreen(viewModel = viewModel)
        }
        composable(Screen.Tables.route) {
            val viewModel: TablesViewModel = hiltViewModel()
            TablesScreen(viewModel = viewModel)
        }
        composable(Screen.Customers.route) {
            val viewModel: com.yach.kinetix.frontofhouse.ui.CustomersViewModel = hiltViewModel()
            com.yach.kinetix.crm.ui.CrmScreen(viewModel = viewModel)
        }
        composable(Screen.SplitCheck.route) {
            val viewModel: SplitCheckViewModel = hiltViewModel()
            SplitCheckScreen(viewModel = viewModel)
        }
        composable(Screen.Kds.route) {
            val viewModel: com.yach.kinetix.backofhouse.ui.KdsViewModel = hiltViewModel()
            com.yach.kinetix.kds.ui.KdsScreen(viewModel = viewModel)
        }
        composable(Screen.Treasury.route) {
            val viewModel: TreasuryViewModel = hiltViewModel()
            TreasuryScreen(viewModel = viewModel)
        }
        composable(Screen.Expenses.route) {
            val viewModel: com.yach.kinetix.treasury.ui.ExpensesViewModel = hiltViewModel()
            com.yach.kinetix.expenses.ui.ExpensesScreen(viewModel = viewModel)
        }
        composable(Screen.Backoffice.route) {
            val viewModel: BackofficeViewModel = hiltViewModel()
            BackofficeScreen(viewModel = viewModel)
        }
        composable(Screen.Settings.route) {
            val viewModel: com.yach.kinetix.backoffice.ui.SettingsViewModel = hiltViewModel()
            com.yach.kinetix.backoffice.ui.SettingsScreen(viewModel = viewModel)
        }
    }
}

