import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class Car(
    val id: Int,
    val licensePlate: String,
    val brand: String,
    val model: String,
    val year: Int,
    val driverName: String,
    val driverLicense: String,
    val status: String,
    val mileage: Int,
    val dailyRate: Double,
    val lastMaintenanceDate: String
)

object JsonUtil {
    fun carToJson(car: Car): String = """  {
    "id": ${car.id},
    "licensePlate": "${esc(car.licensePlate)}",
    "brand": "${esc(car.brand)}",
    "model": "${esc(car.model)}",
    "year": ${car.year},
    "driverName": "${esc(car.driverName)}",
    "driverLicense": "${esc(car.driverLicense)}",
    "status": "${esc(car.status)}",
    "mileage": ${car.mileage},
    "dailyRate": ${car.dailyRate},
    "lastMaintenanceDate": "${esc(car.lastMaintenanceDate)}"
  }"""

    fun carsToJson(cars: List<Car>): String = "[\n${cars.joinToString(",\n") { carToJson(it) }}\n]"

    fun jsonToCars(text: String): List<Car> {
        val result = mutableListOf<Car>()
        val blocks = mutableListOf<String>()
        var depth = 0;
        var start = -1
        for (i in text.indices) {
            when (text[i]) {
                '{' -> {
                    if (depth == 0) start = i; depth++
                }

                '}' -> {
                    depth--; if (depth == 0 && start >= 0) {
                        blocks.add(text.substring(start, i + 1)); start = -1
                    }
                }
            }
        }
        for (block in blocks) {
            try {
                result.add(
                    Car(
                        id = intField(block, "id"),
                        licensePlate = strField(block, "licensePlate"),
                        brand = strField(block, "brand"),
                        model = strField(block, "model"),
                        year = intField(block, "year"),
                        driverName = strField(block, "driverName"),
                        driverLicense = strField(block, "driverLicense"),
                        status = strField(block, "status"),
                        mileage = intField(block, "mileage"),
                        dailyRate = doubleField(block, "dailyRate"),
                        lastMaintenanceDate = strField(block, "lastMaintenanceDate")
                    )
                )
            } catch (e: Exception) {
                println("  [!] Ошибка парсинга JSON-объекта: ${e.message}")
            }
        }
        return result
    }

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

    private fun strField(block: String, key: String): String {
        val regex = Regex(""""$key"\s*:\s*"((?:[^"\\]|\\.)*)"""")
        return regex.find(block)?.groupValues?.get(1) ?: ""
    }

    private fun intField(block: String, key: String): Int {
        val regex = Regex(""""$key"\s*:\s*(-?\d+)""")
        return regex.find(block)?.groupValues?.get(1)?.toInt() ?: 0
    }

    private fun doubleField(block: String, key: String): Double {
        val regex = Regex(""""$key"\s*:\s*(-?[\d.]+)""")
        return regex.find(block)?.groupValues?.get(1)?.toDouble() ?: 0.0
    }
}

class TaxiFleetRepository {
    private val cars: MutableList<Car> = mutableListOf()
    private var nextId: Int = 1

    fun getAll(): List<Car> = cars.toList()
    fun getById(id: Int): Car? = cars.find { it.id == id }

    fun add(car: Car): Car {
        val newCar = car.copy(id = nextId++)
        cars.add(newCar)
        return newCar
    }

    fun update(id: Int, updated: Car): Boolean {
        val index = cars.indexOfFirst { it.id == id }
        if (index == -1) return false
        cars[index] = updated.copy(id = id)
        return true
    }

    fun delete(id: Int): Boolean = cars.removeIf { it.id == id }

    fun searchByDriver(query: String): List<Car> =
        cars.filter { it.driverName.contains(query, ignoreCase = true) }

    fun searchByBrand(query: String): List<Car> =
        cars.filter { it.brand.contains(query, ignoreCase = true) || it.model.contains(query, ignoreCase = true) }

    fun searchByStatus(status: String): List<Car> =
        cars.filter { it.status.equals(status, ignoreCase = true) }

    fun sortByMileage(ascending: Boolean = true): List<Car> =
        if (ascending) cars.sortedBy { it.mileage } else cars.sortedByDescending { it.mileage }

    fun sortByYear(ascending: Boolean = true): List<Car> =
        if (ascending) cars.sortedBy { it.year } else cars.sortedByDescending { it.year }

    fun sortByDailyRate(ascending: Boolean = true): List<Car> =
        if (ascending) cars.sortedBy { it.dailyRate } else cars.sortedByDescending { it.dailyRate }

    fun averageMileage(): Double = if (cars.isEmpty()) 0.0 else cars.map { it.mileage }.average()
    fun totalMileage(): Int = cars.sumOf { it.mileage }
    fun maxMileage(): Int? = cars.maxOfOrNull { it.mileage }
    fun minMileage(): Int? = cars.minOfOrNull { it.mileage }
    fun averageDailyRate(): Double = if (cars.isEmpty()) 0.0 else cars.map { it.dailyRate }.average()
    fun totalDailyRate(): Double = cars.sumOf { it.dailyRate }

    fun loadFromJson(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        val loaded = JsonUtil.jsonToCars(file.readText())
        cars.clear()
        cars.addAll(loaded)
        nextId = (cars.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun saveToJson(filePath: String) {
        File(filePath).writeText(JsonUtil.carsToJson(cars))
    }

    fun loadFromCsv(filePath: String) {
        val file = File(filePath)
        if (!file.exists()) return
        cars.clear()
        for (line in file.readLines().drop(1)) {
            if (line.isBlank()) continue
            val p = line.split(";")
            if (p.size < 11) continue
            try {
                cars.add(
                    Car(
                        p[0].trim().toInt(), p[1].trim(), p[2].trim(), p[3].trim(),
                        p[4].trim().toInt(), p[5].trim(), p[6].trim(), p[7].trim(),
                        p[8].trim().toInt(), p[9].trim().toDouble(), p[10].trim()
                    )
                )
            } catch (e: Exception) {
                println("  [!] Ошибка строки CSV: $line")
            }
        }
        nextId = (cars.maxOfOrNull { it.id } ?: 0) + 1
    }

    fun saveToCsv(filePath: String) {
        val header =
            "id;licensePlate;brand;model;year;driverName;driverLicense;status;mileage;dailyRate;lastMaintenanceDate"
        val rows = cars.joinToString("\n") {
            "${it.id};${it.licensePlate};${it.brand};${it.model};${it.year};" +
                    "${it.driverName};${it.driverLicense};${it.status};${it.mileage};${it.dailyRate};${it.lastMaintenanceDate}"
        }
        File(filePath).writeText("$header\n$rows")
    }

    fun initSampleData() {
        if (cars.isNotEmpty()) return
        listOf(
            Car(
                0,
                "A123BV77",
                "Toyota",
                "Camry",
                2020,
                "Иванов Иван Иванович",
                "7712 345678",
                "Активен",
                85000,
                3500.0,
                "2024-10-15"
            ),
            Car(
                0,
                "B456GD77",
                "Hyundai",
                "Solaris",
                2021,
                "Петров Пётр Петрович",
                "9901 112233",
                "Активен",
                62000,
                2800.0,
                "2024-11-01"
            ),
            Car(
                0,
                "E789ZH99",
                "Kia",
                "Rio",
                2019,
                "Сидоров Сергей Сергеевич",
                "5577 998877",
                "На ТО",
                120000,
                2500.0,
                "2025-01-10"
            ),
            Car(
                0,
                "I012KL77",
                "Volkswagen",
                "Polo",
                2022,
                "Кузнецов Алексей Дмитриевич",
                "7755 444455",
                "Активен",
                34000,
                3200.0,
                "2025-02-20"
            ),
            Car(
                0,
                "M345NO99",
                "Skoda",
                "Rapid",
                2020,
                "Новиков Дмитрий Александрович",
                "3344 667788",
                "Неисправен",
                95000,
                2700.0,
                "2024-09-05"
            ),
            Car(
                0,
                "P678RS77",
                "Renault",
                "Logan",
                2018,
                "Морозов Андрей Викторович",
                "1122 334455",
                "Активен",
                155000,
                2300.0,
                "2025-03-01"
            ),
            Car(
                0,
                "T901UF50",
                "Lada",
                "Vesta",
                2023,
                "Волков Николай Павлович",
                "6688 001122",
                "Активен",
                18000,
                2600.0,
                "2025-04-01"
            ),
        ).forEach { add(it) }
    }
}

object UI {
    private val LINE = "=".repeat(80)
    private val THIN = "-".repeat(80)

    fun header(title: String) {
        println("\n$LINE")
        println("  $title")
        println(LINE)
    }

    fun subHeader(title: String) {
        println("\n$THIN")
        println("  $title")
        println(THIN)
    }

    fun success(msg: String) = println("  [OK] $msg")
    fun error(msg: String) = println("  [ОШИБКА] $msg")
    fun info(msg: String) = println("  [i] $msg")

    fun prompt(msg: String): String {
        print("  > $msg: ")
        return readLine()?.trim() ?: ""
    }

    fun printTable(cars: List<Car>) {
        if (cars.isEmpty()) {
            info("Список пуст."); return
        }
        println()
        println(
            "  %-4s %-12s %-12s %-12s %-4s %-26s %-10s %-12s %-10s".format(
                "ID", "Гос.номер", "Марка", "Модель", "Год", "Водитель", "Пробег(км)", "Статус", "Тариф(руб)"
            )
        )
        println("  " + "-".repeat(112))
        for (c in cars) {
            println(
                "  %-4d %-12s %-12s %-12s %-4d %-26s %-10d %-12s %-10.2f".format(
                    c.id, c.licensePlate, c.brand, c.model, c.year,
                    c.driverName.take(26), c.mileage, c.status, c.dailyRate
                )
            )
        }
        println("  " + "-".repeat(112))
        println("  Итого записей: ${cars.size}")
    }

    fun printCarDetail(car: Car) {
        println()
        println("  ID:                ${car.id}")
        println("  Гос. номер:        ${car.licensePlate}")
        println("  Марка / Модель:    ${car.brand} ${car.model}")
        println("  Год выпуска:       ${car.year}")
        println("  Водитель:          ${car.driverName}")
        println("  Удостоверение:     ${car.driverLicense}")
        println("  Статус:            ${car.status}")
        println("  Пробег:            ${car.mileage} км")
        println("  Тариф за смену:    ${car.dailyRate} руб.")
        println("  Последнее ТО:      ${car.lastMaintenanceDate}")
    }

    fun menu(title: String, items: List<String>) {
        subHeader(title)
        items.forEachIndexed { i, item -> println("  [${i + 1}] $item") }
        println("  [0] Назад")
        println()
    }
}

class TaxiFleetService(private val repo: TaxiFleetRepository) {

    fun showAll() {
        UI.header("СПИСОК АВТОМОБИЛЕЙ ТАКСОПАРКА")
        UI.printTable(repo.getAll())
    }

    fun addCar() {
        UI.header("ДОБАВЛЕНИЕ НОВОГО АВТОМОБИЛЯ")
        try {
            val licensePlate = UI.prompt("Гос. номер")
                .also { require(it.isNotBlank()) { "Гос. номер не может быть пустым" } }
            val brand = UI.prompt("Марка")
                .also { require(it.isNotBlank()) { "Марка не может быть пустой" } }
            val model = UI.prompt("Модель")
                .also { require(it.isNotBlank()) { "Модель не может быть пустой" } }
            val year = UI.prompt("Год выпуска").toIntOrNull()
                ?: throw IllegalArgumentException("Год должен быть числом")
            require(year in 2011..LocalDate.now().year) { "Некорректный год: $year" }
            val driverName = UI.prompt("ФИО водителя")
                .also { require(it.isNotBlank()) { "ФИО не может быть пустым" } }
            val driverLicense = UI.prompt("Номер водительского удостоверения")
                .also { require(it.isNotBlank()) { "Номер не может быть пустым" } }
            val status = chooseStatus() ?: return
            val mileage = UI.prompt("Пробег (км)").toIntOrNull()
                ?: throw IllegalArgumentException("Пробег должен быть числом")
            require(mileage >= 0) { "Пробег не может быть отрицательным" }
            val dailyRate = UI.prompt("Тариф за смену (руб.)").replace(",", ".").toDoubleOrNull()
                ?: throw IllegalArgumentException("Тариф должен быть числом")
            require(dailyRate > 0) { "Тариф должен быть больше нуля" }
            val lastMaintenanceDate = UI.prompt("Дата последнего ТО (ГГГГ-ММ-ДД)")
                .also { validateDate(it) }

            val added = repo.add(
                Car(
                    0, licensePlate, brand, model, year, driverName,
                    driverLicense, status, mileage, dailyRate, lastMaintenanceDate
                )
            )
            UI.success("Автомобиль добавлен с ID = ${added.id}")
        } catch (e: Exception) {
            UI.error(e.message ?: "Неизвестная ошибка")
        }
    }

    fun editCar() {
        UI.header("РЕДАКТИРОВАНИЕ ЗАПИСИ")
        val id = UI.prompt("Введите ID автомобиля").toIntOrNull()
        if (id == null) {
            UI.error("ID должен быть числом"); return
        }
        val car = repo.getById(id) ?: run { UI.error("Автомобиль с ID=$id не найден"); return }
        UI.printCarDetail(car)
        println("\n  (Оставьте поле пустым, чтобы не изменять)\n")
        try {
            val licensePlate = UI.prompt("Гос. номер [${car.licensePlate}]").ifBlank { car.licensePlate }
            val brand = UI.prompt("Марка [${car.brand}]").ifBlank { car.brand }
            val model = UI.prompt("Модель [${car.model}]").ifBlank { car.model }
            val yearStr = UI.prompt("Год выпуска [${car.year}]")
            val year = if (yearStr.isBlank()) car.year
            else yearStr.toIntOrNull() ?: throw IllegalArgumentException("Год должен быть числом")
            val driverName = UI.prompt("ФИО водителя [${car.driverName}]").ifBlank { car.driverName }
            val driverLicense = UI.prompt("Удостоверение [${car.driverLicense}]").ifBlank { car.driverLicense }
            val statusInput = UI.prompt("Статус (1-Активен, 2-На ТО, 3-Неисправен) [${car.status}]")
            val status = when (statusInput) {
                "1" -> "Активен"; "2" -> "На ТО"; "3" -> "Неисправен"; "" -> car.status
                else -> throw IllegalArgumentException("Некорректный статус")
            }
            val mileageStr = UI.prompt("Пробег (км) [${car.mileage}]")
            val mileage = if (mileageStr.isBlank()) car.mileage
            else mileageStr.toIntOrNull() ?: throw IllegalArgumentException("Пробег должен быть числом")
            val rateStr = UI.prompt("Тариф за смену [${car.dailyRate}]")
            val dailyRate = if (rateStr.isBlank()) car.dailyRate
            else rateStr.replace(",", ".").toDoubleOrNull()
                ?: throw IllegalArgumentException("Тариф должен быть числом")
            val dateStr = UI.prompt("Дата последнего ТО [${car.lastMaintenanceDate}]")
            val lastMaintenanceDate = if (dateStr.isBlank()) car.lastMaintenanceDate
            else dateStr.also { validateDate(it) }

            repo.update(
                id, Car(
                    id, licensePlate, brand, model, year, driverName,
                    driverLicense, status, mileage, dailyRate, lastMaintenanceDate
                )
            )
            UI.success("Запись ID=$id успешно обновлена")
        } catch (e: Exception) {
            UI.error(e.message ?: "Неизвестная ошибка")
        }
    }

    fun deleteCar() {
        UI.header("УДАЛЕНИЕ ЗАПИСИ")
        val id = UI.prompt("Введите ID автомобиля для удаления").toIntOrNull()
        if (id == null) {
            UI.error("ID должен быть числом"); return
        }
        val car = repo.getById(id) ?: run { UI.error("Автомобиль с ID=$id не найден"); return }
        UI.printCarDetail(car)
        val confirm = UI.prompt("Подтвердите удаление (да/нет)")
        if (confirm.lowercase() in listOf("да", "yes", "y")) {
            repo.delete(id); UI.success("Автомобиль ID=$id удалён")
        } else {
            UI.info("Удаление отменено")
        }
    }

    fun searchMenu() {
        UI.header("ПОИСК")
        UI.menu("Тип поиска", listOf("По имени водителя", "По марке / модели", "По статусу"))
        when (UI.prompt("Ваш выбор")) {
            "1" -> UI.printTable(repo.searchByDriver(UI.prompt("Часть имени водителя")))
            "2" -> UI.printTable(repo.searchByBrand(UI.prompt("Марка или модель")))
            "3" -> {
                val s = chooseStatus() ?: return; UI.printTable(repo.searchByStatus(s))
            }

            else -> UI.info("Неверный выбор")
        }
    }

    fun sortMenu() {
        UI.header("СОРТИРОВКА")
        UI.menu(
            "Сортировать по", listOf(
                "Пробегу (возрастание)", "Пробегу (убывание)",
                "Году выпуска (возрастание)", "Году выпуска (убывание)",
                "Тарифу (возрастание)", "Тарифу (убывание)"
            )
        )
        val result = when (UI.prompt("Ваш выбор")) {
            "1" -> repo.sortByMileage(true); "2" -> repo.sortByMileage(false)
            "3" -> repo.sortByYear(true); "4" -> repo.sortByYear(false)
            "5" -> repo.sortByDailyRate(true); "6" -> repo.sortByDailyRate(false)
            else -> {
                UI.info("Неверный выбор"); return
            }
        }
        UI.printTable(result)
    }

    fun statsMenu() {
        UI.header("АГРЕГИРОВАННЫЕ ПОКАЗАТЕЛИ")
        if (repo.getAll().isEmpty()) {
            UI.info("Нет данных."); return
        }
        println()
        println("  --- Пробег ---")
        println("  Средний пробег:    ${"%.1f".format(repo.averageMileage())} км")
        println("  Суммарный пробег:  ${repo.totalMileage()} км")
        println("  Максимальный:      ${repo.maxMileage()} км")
        println("  Минимальный:       ${repo.minMileage()} км")
        println()
        println("  --- Тариф за смену ---")
        println("  Средний тариф:     ${"%.2f".format(repo.averageDailyRate())} руб.")
        println("  Суммарно (в день): ${"%.2f".format(repo.totalDailyRate())} руб.")
        println()
        println("  --- Статусы ---")
        repo.getAll().groupBy { it.status }.forEach { (status, list) ->
            println("  $status: ${list.size} авт.")
        }
    }

    fun saveMenu() {
        UI.header("СОХРАНЕНИЕ ДАННЫХ")
        UI.menu("Формат файла", listOf("JSON", "CSV"))
        when (UI.prompt("Ваш выбор")) {
            "1" -> {
                val p = UI.prompt("Путь [fleet.json]")
                    .ifBlank { "fleet.json" }; repo.saveToJson(p); UI.success("Сохранено в $p")
            }

            "2" -> {
                val p = UI.prompt("Путь [fleet.csv]")
                    .ifBlank { "fleet.csv" }; repo.saveToCsv(p); UI.success("Сохранено в $p")
            }

            else -> UI.info("Неверный выбор")
        }
    }

    fun loadMenu() {
        UI.header("ЗАГРУЗКА ДАННЫХ")
        UI.menu("Формат файла", listOf("JSON", "CSV"))
        when (UI.prompt("Ваш выбор")) {
            "1" -> {
                val p = UI.prompt("Путь [fleet.json]").ifBlank { "fleet.json" }
                if (!File(p).exists()) {
                    UI.error("Файл не найден: $p"); return
                }
                repo.loadFromJson(p); UI.success("Загружено ${repo.getAll().size} записей из $p")
            }

            "2" -> {
                val p = UI.prompt("Путь [fleet.csv]").ifBlank { "fleet.csv" }
                if (!File(p).exists()) {
                    UI.error("Файл не найден: $p"); return
                }
                repo.loadFromCsv(p); UI.success("Загружено ${repo.getAll().size} записей из $p")
            }

            else -> UI.info("Неверный выбор")
        }
    }

    private fun chooseStatus(): String? {
        println("\n  Статус: [1] Активен  [2] На ТО  [3] Неисправен")
        return when (UI.prompt("Выберите статус")) {
            "1" -> "Активен"; "2" -> "На ТО"; "3" -> "Неисправен"
            else -> {
                UI.error("Неверный выбор статуса"); null
            }
        }
    }

    private fun validateDate(date: String) {
        try {
            LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            throw IllegalArgumentException("Некорректная дата '$date'. Формат: ГГГГ-ММ-ДД")
        }
    }
}

fun main() {
    val repo = TaxiFleetRepository()
    val service = TaxiFleetService(repo)

    when {
        File("fleet.json").exists() -> {
            repo.loadFromJson("fleet.json")
            println("[OK] Загружены данные из fleet.json (${repo.getAll().size} записей)")
        }

        File("fleet.csv").exists() -> {
            repo.loadFromCsv("fleet.csv")
            println("[OK] Загружены данные из fleet.csv (${repo.getAll().size} записей)")
        }

        else -> {
            repo.initSampleData()
            println("[i] Файл данных не найден. Загружены демонстрационные данные.")
        }
    }

    while (true) {
        UI.header("СИСТЕМА УЧЁТА АВТОПАРКА ТАКСИ")
        println(
            """
  [1]  Показать все автомобили
  [2]  Добавить автомобиль
  [3]  Редактировать запись
  [4]  Удалить запись
  [5]  Поиск
  [6]  Сортировка
  [7]  Статистика и агрегаты
  [8]  Сохранить данные в файл
  [9]  Загрузить данные из файла
  [0]  Выход
        """.trimIndent()
        )

        when (UI.prompt("Выберите пункт меню")) {
            "1" -> service.showAll()
            "2" -> service.addCar()
            "3" -> service.editCar()
            "4" -> service.deleteCar()
            "5" -> service.searchMenu()
            "6" -> service.sortMenu()
            "7" -> service.statsMenu()
            "8" -> service.saveMenu()
            "9" -> service.loadMenu()
            "0" -> {
                repo.saveToJson("fleet.json")
                println("\n  [OK] Данные сохранены в fleet.json. До свидания!")
                return
            }

            else -> UI.error("Неверный пункт меню. Введите число от 0 до 9.")
        }
    }
}
