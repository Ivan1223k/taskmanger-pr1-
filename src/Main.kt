import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class Task(
    val id: Int,
    var title: String,
    var description: String,
    var priority: String,
    var dueDate: String,
    var isCompleted: Boolean,
    var category: String,
    val createdAt: String
)

val PRIORITIES = listOf("Низкий 🔵", "Средний 🟡", "Высокий 🟠", "Срочный 🔴")
val CATEGORIES = listOf("Работа", "Личное", "Учеба", "Здоровье", "Финансы")

private var tasks = mutableListOf<Task>()
private var nextId = 1
private val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

fun createTask(title: String, description: String, priority: String, dueDate: String, category: String): Task {
    val currentDate = LocalDate.now().format(dateFormatter)
    val task = Task(
        id = nextId++,
        title = title,
        description = description,
        priority = priority,
        dueDate = dueDate,
        isCompleted = false,
        category = category,
        createdAt = currentDate
    )
    tasks.add(task)
    return task
}

fun getAllTasks(): List<Task> = tasks.toList()

fun getTaskById(id: Int): Task? = tasks.find { it.id == id }

fun updateTask(id: Int, updateFunction: (Task) -> Unit): Boolean {
    val task = getTaskById(id)
    return task?.let {
        if (!it.isCompleted) {
            updateFunction(it)
            true
        } else false
    } ?: false
}

fun deleteTask(id: Int): Boolean {
    val task = getTaskById(id)
    return task?.let {
        tasks.remove(it)
        true
    } ?: false
}

fun markTaskAsCompleted(id: Int): Boolean {
    return updateTask(id) { task ->
        task.isCompleted = true
    }
}

fun filterTasksByStatus(completed: Boolean?): List<Task> = when (completed) {
    true -> tasks.filter { it.isCompleted }
    false -> tasks.filter { !it.isCompleted }
    null -> tasks
}

fun searchTasks(query: String): List<Task> {
    val lowerQuery = query.lowercase()
    return tasks.filter {
        it.title.lowercase().contains(lowerQuery) ||
                it.description.lowercase().contains(lowerQuery)
    }
}

fun filterTasksByCategory(category: String): List<Task> =
    tasks.filter { it.category == category }

fun filterTasksByPriority(priority: String): List<Task> =
    tasks.filter { it.priority == priority }

fun getOverdueTasks(): List<Task> {
    val today = LocalDate.now()
    return tasks.filter { task ->
        !task.isCompleted && runCatching {
            LocalDate.parse(task.dueDate, dateFormatter).isBefore(today)
        }.getOrDefault(false)
    }
}

fun getTaskStatistics(): Map<String, Any> {
    val total = tasks.size
    val completed = tasks.count { it.isCompleted }
    val active = total - completed
    val completionPercentage = if (total > 0) (completed.toDouble() / total * 100) else 0.0

    return mapOf(
        "total" to total,
        "completed" to completed,
        "active" to active,
        "completionPercentage" to completionPercentage
    )
}

fun getPriorityDistribution(): Map<String, Int> =
    tasks.groupingBy { it.priority }.eachCount()

fun getCategoryDistribution(): Map<String, Int> =
    tasks.groupingBy { it.category }.eachCount()

fun getOverdueCount(): Int = getOverdueTasks().size

fun validateTitle(title: String): Boolean = title.isNotBlank()

fun validateDate(dateString: String): Boolean =
    runCatching { LocalDate.parse(dateString, dateFormatter) }.isSuccess

fun validatePriority(priority: String): Boolean = priority in PRIORITIES

fun validateCategory(category: String): Boolean = category in CATEGORIES

fun formatTaskForDisplay(task: Task): String {
    val statusEmoji = if (task.isCompleted) "✅" else "⏳"
    val priorityColor = when (task.priority) {
        "Низкий 🔵" -> "\u001B[34m"
        "Средний 🟡" -> "\u001B[33m"
        "Высокий 🟠" -> "\u001B[31m"
        "Срочный 🔴" -> "\u001B[35m"
        else -> "\u001B[0m"
    }
    val resetColor = "\u001B[0m"

    return """
         ─── ЗАДАЧА #${task.id} ─────────────────────────────
         Название: ${task.title}
         Описание: ${task.description.ifBlank { "нет описания" }}
         Категория: ${task.category}
         ${priorityColor}⚡ Приоритет: ${task.priority}$resetColor
         Создана: ${task.createdAt}
         Выполнить до: ${task.dueDate}
         $statusEmoji Статус: ${if (task.isCompleted) "Выполнена" else "Активна"}
         ────────────────────────────────────────────────
    """.trimMargin()
}

fun displayTasksList(tasksToDisplay: List<Task>) {
    if (tasksToDisplay.isEmpty()) {
        println("Задачи не найдены")
        return
    }

    tasksToDisplay.forEach { task ->
        println(formatTaskForDisplay(task))
    }
}

fun readValidatedInput(prompt: String, validation: (String) -> Boolean, errorMessage: String): String {
    while (true) {
        print(prompt)
        val input = readLine()?.trim() ?: ""
        if (validation(input)) {
            return input
        }
        println("❌ $errorMessage")
    }
}

fun selectFromList(options: List<String>, prompt: String): String {
    println(prompt)
    options.forEachIndexed { index, option ->
        println("${index + 1}. $option")
    }

    while (true) {
        print("Выберите вариант (1-${options.size}): ")
        val input = readLine()?.toIntOrNull()
        if (input != null && input in 1..options.size) {
            return options[input - 1]
        }
        println("Пожалуйста, введите число от 1 до ${options.size}")
    }
}

fun addTaskUI() {
    println("\nДОБАВЛЕНИЕ НОВОЙ ЗАДАЧИ")

    val title = readValidatedInput(
        "Введите название задачи: ",
        ::validateTitle,
        "Название не может быть пустым"
    )

    print("Введите описание задачи: ")
    val description = readLine()?.trim() ?: ""

    val priority = selectFromList(PRIORITIES, "Выберите приоритет:")
    val category = selectFromList(CATEGORIES, "Выберите категорию:")

    val dueDate = readValidatedInput(
        "Введите дату выполнения (дд.мм.гггг): ",
        ::validateDate,
        "Неверный формат даты. Используйте дд.мм.гггг"
    )

    val task = createTask(title, description, priority, dueDate, category)
    println("Задача успешно создана!")
    println(formatTaskForDisplay(task))
}

fun viewTasksUI() {
    println("\nПРОСМОТР ЗАДАЧ")
    val filterChoice = selectFromList(
        listOf("Все задачи", "Активные задачи", "Выполненные задачи"),
        "Выберите тип отображения:"
    )

    val tasksToShow = when (filterChoice) {
        "Активные задачи" -> filterTasksByStatus(false)
        "Выполненные задачи" -> filterTasksByStatus(true)
        else -> getAllTasks()
    }

    displayTasksList(tasksToShow)
}

fun editTaskUI() {
    println("\nРЕДАКТИРОВАНИЕ ЗАДАЧИ")
    print("Введите ID задачи для редактирования: ")
    val id = readLine()?.toIntOrNull()

    if (id == null) {
        println("Неверный формат ID")
        return
    }

    val task = getTaskById(id)
    if (task == null) {
        println("Задача с ID $id не найдена")
        return
    }

    if (task.isCompleted) {
        println("Невозможно редактировать выполненную задачу")
        return
    }

    println("Текущие данные задачи:")
    println(formatTaskForDisplay(task))

    println("\nЧто вы хотите изменить?")
    val editChoice = selectFromList(
        listOf("Название", "Описание", "Приоритет", "Категорию", "Дату выполнения", "Отмена"),
        "Выберите поле для редактирования:"
    )

    when (editChoice) {
        "Название" -> {
            val newTitle = readValidatedInput(
                "Новое название: ",
                ::validateTitle,
                "Название не может быть пустым"
            )
            updateTask(id) { it.title = newTitle }
            println("Название обновлено")
        }
        "Описание" -> {
            print("Новое описание: ")
            val newDescription = readLine()?.trim() ?: ""
            updateTask(id) { it.description = newDescription }
            println("Описание обновлено")
        }
        "Приоритет" -> {
            val newPriority = selectFromList(PRIORITIES, "Новый приоритет:")
            updateTask(id) { it.priority = newPriority }
            println("Приоритет обновлен")
        }
        "Категорию" -> {
            val newCategory = selectFromList(CATEGORIES, "Новая категория:")
            updateTask(id) { it.category = newCategory }
            println("Категория обновлена")
        }
        "Дату выполнения" -> {
            val newDueDate = readValidatedInput(
                "Новая дата выполнения (дд.мм.гггг): ",
                ::validateDate,
                "Неверный формат даты"
            )
            updateTask(id) { it.dueDate = newDueDate }
            println("Дата выполнения обновлена")
        }
        "Отмена" -> return
    }
}

fun deleteTaskUI() {
    println("\nУДАЛЕНИЕ ЗАДАЧИ")
    print("Введите ID задачи для удаления: ")
    val id = readLine()?.toIntOrNull()

    if (id == null) {
        println("Неверный формат ID")
        return
    }

    val task = getTaskById(id)
    if (task == null) {
        println("Задача с ID $id не найдена")
        return
    }

    println("Вы собираетесь удалить задачу:")
    println(formatTaskForDisplay(task))

    print("Вы уверены? (да/нет): ")
    val confirmation = readLine()?.trim()?.lowercase()

    if (confirmation == "да" || confirmation == "д") {
        if (deleteTask(id)) {
            println("Задача успешно удалена")
        } else {
            println("Ошибка при удалении задачи")
        }
    } else {
        println("Удаление отменено")
    }
}

fun markTaskCompleteUI() {
    println("\nОТМЕТКА О ВЫПОЛНЕНИИ")
    print("Введите ID выполненной задачи: ")
    val id = readLine()?.toIntOrNull()

    if (id == null) {
        println("Неверный формат ID")
        return
    }

    if (markTaskAsCompleted(id)) {
        println("Задача отмечена как выполненная")
    } else {
        println("Задача не найдена или уже выполнена")
    }
}

fun searchTasksUI() {
    println("\nПОИСК ЗАДАЧ")
    val searchType = selectFromList(
        listOf("По содержимому", "По категории", "По приоритету", "Просроченные задачи", "Отмена"),
        "Выберите тип поиска:"
    )

    when (searchType) {
        "По содержимому" -> {
            print("Введите поисковый запрос: ")
            val query = readLine()?.trim() ?: ""
            val results = searchTasks(query)
            displayTasksList(results)
            println("Найдено задач: ${results.size}")
        }
        "По категории" -> {
            val category = selectFromList(CATEGORIES, "Выберите категорию:")
            val results = filterTasksByCategory(category)
            displayTasksList(results)
            println("Найдено задач: ${results.size}")
        }
        "По приоритету" -> {
            val priority = selectFromList(PRIORITIES, "Выберите приоритет:")
            val results = filterTasksByPriority(priority)
            displayTasksList(results)
            println("Найдено задач: ${results.size}")
        }
        "Просроченные задачи" -> {
            val results = getOverdueTasks()
            displayTasksList(results)
            println("Просроченных задач: ${results.size}")
        }
        "Отмена" -> return
    }
}

fun showAnalyticsUI() {
    println("\nАНАЛИТИКА ЗАДАЧ")

    val stats = getTaskStatistics()
    println("ОБЩАЯ СТАТИСТИКА:")
    println(" Всего задач: ${stats["total"]}")
    println(" Выполнено: ${stats["completed"]}")
    println(" Активных: ${stats["active"]}")
    println(" Процент выполнения: ${"%.1f".format(stats["completionPercentage"])}%")

    println("\nРАСПРЕДЕЛЕНИЕ ПО ПРИОРИТЕТАМ:")
    getPriorityDistribution().forEach { (priority, count) ->
        println("   $priority: $count задач")
    }

    println("\nРАСПРЕДЕЛЕНИЕ ПО КАТЕГОРИЯМ:")
    getCategoryDistribution().forEach { (category, count) ->
        println("   $category: $count задач")
    }

    println("\nПРОСРОЧЕННЫХ ЗАДАЧ: ${getOverdueCount()}")
}


fun showMainMenu() {
    println("\n" + "=".repeat(50))
    println("СИСТЕМА УПРАВЛЕНИЯ ЗАДАЧАМИ")
    println("=".repeat(50))
    println("1.Просмотреть задачи")
    println("2.Добавить задачу")
    println("3.Редактировать задачу")
    println("4.Отметить как выполненную")
    println("5.Удалить задачу")
    println("6.Поиск и фильтрация")
    println("7.Аналитика")
    println("8.Выход")
    println("=".repeat(50))
}

fun main() {

    createTask(
        "Изучить Kotlin",
        "Освоить основы языка Kotlin",
        "Высокий 🟠",
        "25.12.2024",
        "Учеба"
    )

    createTask(
        "Купить продукты",
        "Молоко, хлеб, фрукты",
        "Средний 🟡",
        "20.12.2024",
        "Личное"
    )

    var running = true
    while (running) {
        showMainMenu()
        print("Выберите действие (1-8): ")

        when (readLine()?.trim()) {
            "1" -> viewTasksUI()
            "2" -> addTaskUI()
            "3" -> editTaskUI()
            "4" -> markTaskCompleteUI()
            "5" -> deleteTaskUI()
            "6" -> searchTasksUI()
            "7" -> showAnalyticsUI()
            "8" -> {
                println("Конец!")
                running = false
            }
            else -> println("Неверный выбор. Пожалуйста, введите число от 1 до 8")
        }

        if (running) {
            print("\nНажмите Enter для продолжения...")
            readLine()
        }
    }
}