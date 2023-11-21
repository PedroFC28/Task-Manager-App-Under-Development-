package com.example.taskmanager

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.taskmanager.ui.theme.TaskManagerTheme
import java.time.LocalDate
import java.time.Month
import java.time.format.TextStyle
import java.util.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.ViewModel
import java.time.Year
import java.time.YearMonth
import androidx.lifecycle.viewmodel.compose.viewModel



// TaskViewModel: Manages the state and logic of tasks within the application.
class TaskViewModel : ViewModel() {
    val tasks = mutableStateListOf<Task>()
}


// Main activity of the application.
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Setting the theme and surface for the application's UI.
            TaskManagerTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val taskViewModel: TaskViewModel = viewModel()
                    AppScreen(taskViewModel)
                }
            }
        }
    }
}

// Main screen of the app, managing navigation between the welcome screen and the task list screen.
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AppScreen(taskViewModel: TaskViewModel) {
    var userName by remember { mutableStateOf("") }
    var showWelcomeScreen by remember { mutableStateOf(true) }
    var showCalendar by remember { mutableStateOf(false) }
    var showTaskListScreen by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // Function to handle back navigation from the task list screen.
    fun goBackFromTaskListScreen() {
        showTaskListScreen = false
        showCalendar = true
    }

    // Conditional rendering of screens based on state variables.
    when {
        showWelcomeScreen -> {
            WelcomeScreen(onNameEntered = { name ->
                userName = name
                showWelcomeScreen = false
                showCalendar = true
            })
        }
        showCalendar -> {
            CalendarScreen(onDateSelected = { date ->
                selectedDate = date
                showCalendar = false

            }, tasks = taskViewModel.tasks )
        }
        else -> {
            TaskListScreen(userName, selectedDate.month, selectedDate.dayOfMonth,onBack = { goBackFromTaskListScreen() }, taskViewModel)
        }
    }
}


// Welcome screen for entering user name.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(onNameEntered: (String) -> Unit) {
    var name by remember { mutableStateOf("") }

    // Layout for the welcome screen with input field and button.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Enter your name") }
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { onNameEntered(name) }) {
            Text("Enter")
        }
    }
}

// Screen for calendar view.
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarScreen(onDateSelected: (LocalDate) -> Unit, tasks: List<Task>) {
    // State variables for year and month selection.
    var selectedYear by remember { mutableStateOf(Year.now().value) }
    var selectedMonth by remember { mutableStateOf(LocalDate.now().month) }

    // week days
    val weekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Layout for the calendar screen including the month-year dropdown and grid for days.
    Column {
        MonthYearDropdownMenu(
            currentYear = selectedYear,
            currentMonth = selectedMonth,
            onYearChange = { selectedYear = it },
            onMonthChange = { selectedMonth = it }
        )

        // Header added with week days
        Row(modifier = Modifier.fillMaxWidth()) {
            for (day in weekDays) {
                Text(
                    text = day,
                    modifier = Modifier
                        .weight(1f)
                        .padding(8.dp),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }




        // Calculate days in the selected month and layout for each day.
        val daysInMonth = YearMonth.of(selectedYear, selectedMonth.value).lengthOfMonth()
        val firstDayOfMonth = LocalDate.of(selectedYear, selectedMonth.value, 1).dayOfWeek.value

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.padding(8.dp)) {
            items(firstDayOfMonth - 1) { Spacer(modifier = Modifier.size(48.dp)) }
            items(daysInMonth) { day ->
                //Check if day have any task
                val dayHasTask = tasks.any { task -> task.day == day && task.month == selectedMonth}
                DayCell(day = day + 1, hasTasks = dayHasTask) { onDateSelected(LocalDate.of(selectedYear, selectedMonth.value, day + 1)) }
            }
        }
    }
}

// Screen for displaying and managing tasks.
@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskListScreen(userName: String, selectedMonth: Month, selectedDay: Int, onBack: () -> Unit,taskViewModel: TaskViewModel) {
    var taskToEdit by remember { mutableStateOf<Task?>(null) }




    // Sorted list of tasks, important ones first.
    val sortedTasks by remember(selectedMonth, selectedDay) {
        derivedStateOf {
            taskViewModel.tasks.filter { it.month == selectedMonth && it.day == selectedDay }
                .sortedByDescending { it.isImportant }
        }
    }

    // State variables for managing task addition and editing dialog.
    var showDialog by remember { mutableStateOf(false) }
    var newTaskTitle by remember { mutableStateOf("") }
    var newTaskDescription by remember { mutableStateOf("") }
    var isTaskImportant by remember { mutableStateOf(false) }

    val setShowDialog = { value: Boolean ->
        showDialog = value
    }



    // Show dialog for adding or editing tasks.
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(taskToEdit?.title ?: "New Task") },
            text = {
                Column {
                    TextField(
                        value = newTaskTitle,
                        onValueChange = { newTaskTitle = it },
                        label = { Text("Title") }
                    )
                    TextField(
                        value = newTaskDescription,
                        onValueChange = { newTaskDescription = it },
                        label = { Text("Description") }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = isTaskImportant,
                            onCheckedChange = { isTaskImportant = it }
                        )
                        Text("Important Task")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Update existing task or add new one.
                        if (taskToEdit != null) {
                            val taskIndex = taskViewModel.tasks.indexOfFirst { it.title == taskToEdit!!.title && it.description == taskToEdit!!.description && it.day == taskToEdit!!.day && it.month == taskToEdit!!.month }
                            if (taskIndex != -1) {
                                taskViewModel.tasks[taskIndex] = Task(
                                    newTaskTitle,
                                    newTaskDescription,
                                    isTaskImportant,
                                    selectedMonth,
                                    selectedDay
                                )
                            }
                        } else {
                            taskViewModel.tasks.add(
                                Task(
                                    newTaskTitle,
                                    newTaskDescription,
                                    isTaskImportant,
                                    selectedMonth,
                                    selectedDay
                                )
                            )
                        }
                        // Reset dialog state.
                        taskToEdit = null
                        newTaskTitle = ""
                        newTaskDescription = ""
                        isTaskImportant = false
                        showDialog = false
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Main content area with task list.
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            "Welcome, ${userName.ifBlank { "Guest" }}",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
        Button(onClick = onBack) {
            Icon(Icons.Filled.ArrowBack, contentDescription = "Retroceder")
            Text("Retroceder")
        }

        TaskArea(
            selectedMonth,
            selectedDay,
            sortedTasks,
            setShowDialog = setShowDialog,
            onTaskEditClick = { task ->
                // Prepare for editing the task
                taskToEdit = task
                newTaskTitle = task.title
                newTaskDescription = task.description
                isTaskImportant = task.isImportant
                showDialog = true
            },
            onTaskRemoveClick = { task ->
                // Remove the task
                taskViewModel.tasks.remove(task)
                setShowDialog(false)
            }
        )
    }
}

// Function to display the task area with a list of tasks.
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TaskArea(
    month: Month, day: Int, sortedTasks: List<Task>,onTaskRemoveClick: (Task) -> Unit ,onTaskEditClick: (Task) -> Unit,setShowDialog: (Boolean)->Unit) {

    Column(modifier = Modifier.fillMaxSize()) {
        // Displaying the date and 'Add Task' button.
        Text(
            text = "Tasks for ${month.getDisplayName(TextStyle.FULL, Locale.getDefault())} $day",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        // Button to add a new task.
        Button(onClick = {setShowDialog(true)}) {
            Text("Add Task")
        }

        // Lazy column for tasks to improve performance with a large number of tasks.
        LazyColumn {
            items(sortedTasks) { task ->
                // Display each task item with edit and remove functionality.
                TaskItem(task = task,
                    onEditClick = {onTaskEditClick(task)},
                    onRemoveClick = {onTaskRemoveClick(task)})
            }
        }
    }
}

// Composable function to display individual task items.
@Composable
fun TaskItem(task: Task, onEditClick: () -> Unit, onRemoveClick: () -> Unit) {

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(10.dp)
            .background(if (task.isImportant) Color.Red else Color.White)
            .clickable(onClick = onEditClick)
        ){
        // Layout for displaying task title and description.
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = task.title, fontWeight = FontWeight.Bold)
            Text(text = task.description)

        }
        // Button to remove a task
        IconButton(onClick = { onRemoveClick() }) { // Remover quando clicar no botão
            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete Task")
        }
    }
}

// Composable function to display a calendar day cell.
@Composable
fun DayCell(day: Int, hasTasks: Boolean, onClick: () -> Unit) {
    // Styling for each day in the calendar.
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(8.dp)
            .size(48.dp)
            .clip(CircleShape)
            .background(if (hasTasks) Color.Red else MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        Text(text = day.toString())
    }
}


// Composable function for the month and year dropdown menu.
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun MonthYearDropdownMenu(
    currentYear: Int,
    currentMonth: Month,
    onYearChange: (Int) -> Unit,
    onMonthChange: (Month) -> Unit
) {
    // Arrays for months and years to be displayed in dropdown.
    val months = Month.values()
    val years = (currentYear -3 ..currentYear + 3).toList()
    var showMonthDialog by remember { mutableStateOf(false) }
    var showYearDialog by remember { mutableStateOf(false) }

    val listAsString = years.joinToString(separator = ", ", prefix = "[", postfix = "]")
    Log.d("MyTag", "List content: $listAsString")

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        // Button to select the month.
        Button(onClick = { showMonthDialog = true }) {
            Text(text = currentMonth.getDisplayName(TextStyle.FULL, Locale.getDefault()))
        }


        // Dialog for selecting a month.
        if (showMonthDialog) {
            AlertDialog(
                onDismissRequest = { showMonthDialog = false },
                title = { Text(text = "Choose Month") },
                text = {
                    Column {
                        months.forEach { month ->
                            TextButton(onClick = {
                                onMonthChange(month)
                                showMonthDialog = false
                            }) {
                                Text(text = month.getDisplayName(TextStyle.FULL, Locale.getDefault()))
                            }
                        }
                    }
                },
                confirmButton = { }
            )
        }

        // Button to select the year.
        Button(onClick = { showYearDialog = true }) {
            Text(text = currentYear.toString())
        }

        // Dialog for selecting a year.
        if (showYearDialog) {
            AlertDialog(
                onDismissRequest = { showYearDialog= false },
                title = { Text(text = "Choose Year") },
                text = {
                    Column {
                        years.forEach { year ->
                            TextButton(onClick = {
                                onYearChange(year)
                                showYearDialog = false
                            }) {
                                Text(text = year.toString())
                            }
                        }
                    }
                },
                confirmButton = { }
            )
        }
    }
}

// Data class representing a task.
data class Task(val title: String, val description: String, val isImportant: Boolean, val month: Month, val day: Int)

