package com.ailenezareti.nezaretv4.ui

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ailenezareti.nezaretv4.Prefs
import com.ailenezareti.nezaretv4.api.ApiClient
import com.ailenezareti.nezaretv4.databinding.FragmentMapBinding
import com.ailenezareti.nezaretv4.model.LocationPoint
import com.google.android.material.bottomsheet.BottomSheetBehavior
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.*

class MapFragment : Fragment() {
    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private var lastPoint: GeoPoint? = null
    private var lastShareText = ""
    private var cachedToday: List<LocationPoint> = emptyList()
    private var routeVisible = false
    private lateinit var sheetBehavior: BottomSheetBehavior<View>

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        Configuration.getInstance().userAgentValue = requireContext().packageName
        binding.map.setTileSource(TileSourceFactory.MAPNIK)
        binding.map.setMultiTouchControls(true)
        binding.map.controller.setZoom(17.0)

        sheetBehavior = BottomSheetBehavior.from(binding.sheet)
        sheetBehavior.peekHeight = dp(112)
        sheetBehavior.isHideable = false
        sheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED

        binding.refresh.setOnClickListener { loadLatest(true) }
        binding.quickRefresh.setOnClickListener { loadLatest(true) }
        binding.target.setOnClickListener { showLatestOnly() }
        binding.quickLatest.setOnClickListener { showLatestOnly() }
        binding.route.setOnClickListener { toggleTodayRoute() }
        binding.quickRoute.setOnClickListener { toggleTodayRoute() }
        binding.history.setOnClickListener { chooseHistoryDate() }
        binding.quickHistory.setOnClickListener { chooseHistoryDate() }
        binding.share.setOnClickListener { shareLocation() }
        binding.quickShare.setOnClickListener { shareLocation() }
        binding.layers.setOnClickListener { showLayerMenu() }

        loadLatest(false)
    }

    private fun loadLatest(showMessage: Boolean) {
        val childId = Prefs.child(requireContext())
        if (childId < 0) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.service(requireContext())
                cachedToday = api.locations(childId, "today").body()?.locations.orEmpty().sortedBy { it.recorded_at }
                val children = api.children().body()?.children.orEmpty()
                val child = children.firstOrNull { it.id == childId } ?: children.firstOrNull()
                binding.mapChild.text = child?.name ?: "Uşaq"
                binding.mapAvatar.text = child?.name?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "U"
                updateSummary(cachedToday)
                showLatestOnly()
                if (showMessage) Toast.makeText(requireContext(), "Son mövqe yeniləndi", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Xəritə məlumatı yüklənmədi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLatestOnly() {
        val latest = cachedToday.lastOrNull() ?: return
        routeVisible = false
        binding.map.overlays.clear()
        val point = latest.toGeoPoint() ?: return
        lastPoint = point
        addPin(point, Color.rgb(109, 76, 246), "Son nöqtə", true)
        binding.map.controller.setZoom(17.0)
        binding.map.controller.animateTo(point)
        updateLatestMeta(latest)
        binding.map.invalidate()
    }

    private fun toggleTodayRoute() {
        if (routeVisible) {
            showLatestOnly()
            return
        }
        drawRoute(cachedToday, "Bugünkü marşrut")
    }

    private fun drawRoute(raw: List<LocationPoint>, title: String) {
        val valid = filterGpsJumps(raw)
        if (valid.size < 2) {
            Toast.makeText(requireContext(), "Marşrut üçün kifayət qədər GPS nöqtəsi yoxdur", Toast.LENGTH_SHORT).show()
            return
        }
        routeVisible = true
        binding.map.overlays.clear()
        val points = valid.mapNotNull { it.toGeoPoint() }
        val line = Polyline().apply {
            setPoints(points)
            outlinePaint.strokeWidth = 7f
            outlinePaint.color = Color.rgb(109, 76, 246)
        }
        binding.map.overlays.add(line)

        // Köhnə prinsip: keçmiş nöqtələr görünür, başlanğıc yaşıl, son qırmızı.
        points.drop(1).dropLast(1).forEachIndexed { index, p ->
            if (index % max(1, points.size / 45) == 0) addDot(p)
        }
        addPin(points.first(), Color.rgb(25, 170, 103), "Başlanğıc", false)
        addPin(points.last(), Color.rgb(225, 57, 57), "Son nöqtə", true)
        lastPoint = points.last()
        updateLatestMeta(valid.last())
        zoomToRoute(points)
        binding.map.invalidate()
        Toast.makeText(requireContext(), title, Toast.LENGTH_SHORT).show()
    }

    private fun chooseHistoryDate() {
        val now = LocalDate.now()
        DatePickerDialog(requireContext(), { _, y, m, d ->
            val date = LocalDate.of(y, m + 1, d)
            loadHistory(date)
        }, now.year, now.monthValue - 1, now.dayOfMonth).show()
    }

    private fun loadHistory(date: LocalDate) {
        val childId = Prefs.child(requireContext())
        if (childId < 0) return
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.service(requireContext())
                val list = api.locations(childId, "custom", date.toString(), date.toString()).body()?.locations.orEmpty().sortedBy { it.recorded_at }
                drawRoute(list, "Tarixçə: $date")
                updateSummary(list)
            } catch (_: Exception) {
                Toast.makeText(requireContext(), "Tarixçə yüklənmədi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLayerMenu() {
        val names = arrayOf("Standart", "Yüngül görünüş")
        AlertDialog.Builder(requireContext())
            .setTitle("Xəritə görünüşü")
            .setSingleChoiceItems(names, 0) { dialog, which ->
                // OSM-in açarsız stabil mənbəyi saxlanılır; ikinci seçim zoom/overlayləri təmizləmir.
                binding.map.setTileSource(TileSourceFactory.MAPNIK)
                binding.map.invalidate()
                dialog.dismiss()
            }.show()
    }

    private fun shareLocation() {
        if (lastShareText.isBlank()) return
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, lastShareText)
        }, "Mövqeyi paylaş"))
    }

    private fun updateLatestMeta(x: LocationPoint) {
        binding.mapAddress.text = "${x.latitude}, ${x.longitude}"
        binding.mapMeta.text = "Batareya ${x.battery_pct?.let { "$it%" } ?: "—"}   •   Dəqiqlik ${x.accuracy_m ?: "—"} m"
        lastShareText = "${binding.mapChild.text}: ${x.latitude}, ${x.longitude}"
    }

    private fun updateSummary(list: List<LocationPoint>) {
        val filtered = filterGpsJumps(list)
        var meters = 0.0
        val points = filtered.mapNotNull { it.toGeoPoint() }
        for (i in 1 until points.size) meters += distance(points[i - 1], points[i])
        val lastTime = list.lastOrNull()?.recorded_at?.let { shortTime(it) } ?: "—"
        binding.mapSummary.text = "◷  Getdiyi məsafə                         ${"%.2f".format(meters / 1000.0)} km\n" +
                "◎  GPS qeydləri                           ${list.size}\n" +
                "◉  Son yenilənmə                          $lastTime"
    }

    private fun filterGpsJumps(input: List<LocationPoint>): List<LocationPoint> {
        val sorted = input.sortedBy { it.recorded_at }
        if (sorted.size < 2) return sorted
        val out = mutableListOf<LocationPoint>()
        for (item in sorted) {
            val p = item.toGeoPoint() ?: continue
            val previous = out.lastOrNull()?.toGeoPoint()
            if (previous == null) {
                out += item
                continue
            }
            val meters = distance(previous, p)
            val seconds = secondsBetween(out.last().recorded_at, item.recorded_at).coerceAtLeast(1)
            val speedKmh = meters / seconds * 3.6
            // Ailə izləmə üçün qeyri-real GPS sıçrayışlarını marşrutdan çıxarırıq.
            if (meters <= 1200 || speedKmh <= 180) out += item
        }
        return out
    }

    private fun secondsBetween(a: String, b: String): Long = try {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        java.time.Duration.between(LocalDateTime.parse(a, fmt), LocalDateTime.parse(b, fmt)).seconds
    } catch (_: Exception) { 1L }

    private fun shortTime(value: String): String = if (value.length >= 16) value.substring(11, 16) else value.takeLast(5)

    private fun LocationPoint.toGeoPoint(): GeoPoint? = try { GeoPoint(latitude.toDouble(), longitude.toDouble()) } catch (_: Exception) { null }

    private fun distance(a: GeoPoint, b: GeoPoint): Double {
        val r = 6371000.0
        val p1 = Math.toRadians(a.latitude)
        val p2 = Math.toRadians(b.latitude)
        val dp = Math.toRadians(b.latitude - a.latitude)
        val dl = Math.toRadians(b.longitude - a.longitude)
        val h = sin(dp / 2).pow(2) + cos(p1) * cos(p2) * sin(dl / 2).pow(2)
        return 2 * r * atan2(sqrt(h), sqrt(1 - h))
    }

    private fun addPin(point: GeoPoint, color: Int, title: String, large: Boolean) {
        val marker = Marker(binding.map)
        marker.position = point
        marker.title = title
        marker.icon = makePin(color, if (large) 54 else 46)
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        binding.map.overlays.add(marker)
    }

    private fun addDot(point: GeoPoint) {
        val marker = Marker(binding.map)
        marker.position = point
        marker.icon = makeDot()
        marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
        binding.map.overlays.add(marker)
    }

    private fun makePin(color: Int, sizeDp: Int): android.graphics.drawable.BitmapDrawable {
        val w = dp(sizeDp)
        val h = dp(sizeDp + 14)
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = color
        val cx = w / 2f
        val radius = w * .34f
        canvas.drawCircle(cx, radius + dp(5), radius, paint)
        val path = Path().apply {
            moveTo(cx - radius * .55f, radius * 1.55f)
            lineTo(cx, h - dp(2).toFloat())
            lineTo(cx + radius * .55f, radius * 1.55f)
            close()
        }
        canvas.drawPath(path, paint)
        paint.color = Color.WHITE
        canvas.drawCircle(cx, radius + dp(5), radius * .52f, paint)
        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    private fun makeDot(): android.graphics.drawable.BitmapDrawable {
        val s = dp(12)
        val bitmap = Bitmap.createBitmap(s, s, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(109, 76, 246) }
        canvas.drawCircle(s / 2f, s / 2f, s * .34f, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = dp(1).toFloat()
        paint.color = Color.WHITE
        canvas.drawCircle(s / 2f, s / 2f, s * .34f, paint)
        return android.graphics.drawable.BitmapDrawable(resources, bitmap)
    }

    private fun zoomToRoute(points: List<GeoPoint>) {
        if (points.isEmpty()) return
        try {
            val box = org.osmdroid.util.BoundingBox.fromGeoPoints(points)
            binding.map.zoomToBoundingBox(box, true, dp(70))
        } catch (_: Exception) {
            binding.map.controller.animateTo(points.last())
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()

    override fun onResume() { super.onResume(); binding.map.onResume() }
    override fun onPause() { binding.map.onPause(); super.onPause() }
    override fun onDestroyView() { super.onDestroyView(); _binding = null }
}
