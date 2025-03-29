import com.example.jetpackdemoapp.data.model.model.Main
import com.example.jetpackdemoapp.data.model.model.Weather

data class DailyForecastResponse(
    val list: List<ListItem>
) {
    data class ListItem(
        val dt: Long,
        val main: Main,
        val weather: List<Weather>
    )
}