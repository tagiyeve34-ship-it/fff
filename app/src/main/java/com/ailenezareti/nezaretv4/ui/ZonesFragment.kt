package com.ailenezareti.nezaretv4.ui

import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ailenezareti.nezaretv4.Prefs
import com.ailenezareti.nezaretv4.R
import com.ailenezareti.nezaretv4.api.ApiClient
import com.ailenezareti.nezaretv4.databinding.FragmentZonesBinding
import com.ailenezareti.nezaretv4.databinding.ItemZoneBinding
import com.ailenezareti.nezaretv4.model.GeoZone
import com.ailenezareti.nezaretv4.model.ZoneDeleteRequest
import com.ailenezareti.nezaretv4.model.ZoneSaveRequest
import kotlinx.coroutines.launch

class ZonesFragment : Fragment() {
    private var _binding: FragmentZonesBinding? = null
    private val binding get() = _binding!!

    private val adapter = ZoneAdapter(
        changed = { zone, enabled -> toggle(zone, enabled) },
        clicked = { zone -> showDetail(zone) }
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentZonesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.list.layoutManager = LinearLayoutManager(requireContext())
        binding.list.adapter = adapter
        binding.add.setOnClickListener { showAddDialog() }
        load()
    }

    private fun load() {
        val childId = Prefs.child(requireContext())
        if (childId < 0) return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                adapter.items = ApiClient.service(requireContext())
                    .zones(childId)
                    .body()
                    ?.zones
                    .orEmpty()
                adapter.notifyDataSetChanged()
            } catch (_: Exception) {
            }
        }
    }

    private fun toggle(zone: GeoZone, enabled: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val request = ZoneSaveRequest(
                    id = zone.id,
                    child_id = zone.child_id,
                    name = zone.name,
                    latitude = zone.latitude.toDouble(),
                    longitude = zone.longitude.toDouble(),
                    radius_m = zone.radius_m,
                    notify_enter = zone.notify_enter == 1,
                    notify_exit = zone.notify_exit == 1,
                    is_active = enabled
                )
                ApiClient.service(requireContext()).updateZone(request)
                load()
            } catch (_: Exception) {
            }
        }
    }

    private fun showDetail(zone: GeoZone) {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 20, 44, 8)
        }

        fun addText(text: String, size: Float = 14f, centered: Boolean = false, bold: Boolean = false) {
            val view = TextView(requireContext()).apply {
                this.text = text
                textSize = size
                setTextColor(resources.getColor(R.color.text, null))
                setPadding(0, 10, 0, 10)
                if (centered) gravity = Gravity.CENTER
                if (bold) setTypeface(typeface, Typeface.BOLD)
            }
            box.addView(view)
        }

        addText("⌂", 42f, centered = true)
        addText(zone.name, 22f, centered = true, bold = true)
        addText(if (zone.is_active == 1) "Aktiv" else "Qeyri-aktiv", 14f, centered = true)
        addText(
            "Ünvan / koordinat\n${zone.latitude}, ${zone.longitude}" +
                "\n\nRadius\n${zone.radius_m} m" +
                "\n\nBildiriş" +
                "\nDaxil olduqda: ${if (zone.notify_enter == 1) "Açıq" else "Bağlı"}" +
                "\nÇıxdıqda: ${if (zone.notify_exit == 1) "Açıq" else "Bağlı"}"
        )

        AlertDialog.Builder(requireContext())
            .setView(box)
            .setNegativeButton("Bağla", null)
            .setNeutralButton("Zonanı sil") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        ApiClient.service(requireContext())
                            .deleteZone(ZoneDeleteRequest(zone.id, zone.child_id))
                        load()
                    } catch (_: Exception) {
                    }
                }
            }
            .show()
    }

    private fun showAddDialog() {
        val box = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(36, 16, 36, 0)
        }

        fun input(hint: String): EditText {
            return EditText(requireContext()).also {
                it.hint = hint
                box.addView(it)
            }
        }

        val name = input("Zona adı")
        val latitude = input("Enlik (latitude)")
        val longitude = input("Uzunluq (longitude)")
        val radius = input("Radius, metr")

        AlertDialog.Builder(requireContext())
            .setTitle("Yeni zona")
            .setView(box)
            .setNegativeButton("Ləğv et", null)
            .setPositiveButton("Yadda saxla") { _, _ ->
                val childId = Prefs.child(requireContext())
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        val request = ZoneSaveRequest(
                            child_id = childId,
                            name = name.text.toString(),
                            latitude = latitude.text.toString().toDouble(),
                            longitude = longitude.text.toString().toDouble(),
                            radius_m = radius.text.toString().toInt(),
                            notify_enter = true,
                            notify_exit = true
                        )
                        ApiClient.service(requireContext()).createZone(request)
                        load()
                    } catch (_: Exception) {
                    }
                }
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private class ZoneAdapter(
        val changed: (GeoZone, Boolean) -> Unit,
        val clicked: (GeoZone) -> Unit
    ) : RecyclerView.Adapter<ZoneAdapter.Holder>() {

        var items: List<GeoZone> = emptyList()

        class Holder(val binding: ItemZoneBinding) : RecyclerView.ViewHolder(binding.root)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val binding = ItemZoneBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return Holder(binding)
        }

        override fun getItemCount(): Int = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val zone = items[position]
            holder.binding.name.text = zone.name
            holder.binding.status.text = if (zone.is_active == 1) "Aktiv" else "Qeyri-aktiv"
            holder.binding.meta.text = "${zone.latitude}, ${zone.longitude}\nRadius: ${zone.radius_m} m"

            holder.binding.toggle.setOnCheckedChangeListener(null)
            holder.binding.toggle.isChecked = zone.is_active == 1
            holder.binding.toggle.setOnCheckedChangeListener { _, enabled ->
                changed(zone, enabled)
            }
            holder.binding.root.setOnClickListener { clicked(zone) }
        }
    }
}
