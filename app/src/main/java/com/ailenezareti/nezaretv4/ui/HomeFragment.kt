package com.ailenezareti.nezaretv4.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ailenezareti.nezaretv4.Prefs
import com.ailenezareti.nezaretv4.api.ApiClient
import com.ailenezareti.nezaretv4.databinding.FragmentHomeBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        load()
    }

    private fun load() {
        val childId = Prefs.child(requireContext())
        if (childId < 0) {
            viewLifecycleOwner.lifecycleScope.launch {
                delay(800)
                if (_binding != null) load()
            }
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val api = ApiClient.service(requireContext())

                val children = api.children().body()?.children.orEmpty()
                val child = children.firstOrNull { it.id == childId } ?: children.firstOrNull()

                binding.childName.text = child?.name ?: "Uşaq"
                binding.avatar.text = child?.name
                    ?.trim()
                    ?.firstOrNull()
                    ?.uppercaseChar()
                    ?.toString()
                    ?: "U"
                binding.lastSeen.text = child?.last_seen?.let { "Son yenilənmə: $it" }
                    ?: "Online məlumatı gözlənilir"

                val locations = api.locations(childId, "today").body()?.locations.orEmpty()
                val latestLocation = locations.lastOrNull()
                if (latestLocation != null) {
                    binding.address.text = "${latestLocation.latitude}, ${latestLocation.longitude}"
                    binding.batteryNow.text = latestLocation.battery_pct?.let { "$it%" } ?: "—"
                }

                val batteryValues = locations.mapNotNull { it.battery_pct }
                binding.batteryChart.mode = "battery"
                binding.batteryChart.battery = batteryValues
                binding.batteryChart.invalidate()

                if (batteryValues.isNotEmpty()) {
                    val first = batteryValues.first()
                    val last = batteryValues.last()
                    val delta = last - first
                    binding.batterySummary.text = "$first% → $last%  •  dəyişmə $delta%"
                }

                val today = LocalDate.now().toString()
                val calls = api.calls(childId, today, today).body()?.calls.orEmpty()

                fun matchesType(value: String, vararg names: String): Boolean {
                    return names.any { value.equals(it, ignoreCase = true) }
                }

                val incoming = calls.filter {
                    matchesType(it.call_type, "incoming", "in", "gelen", "1")
                }
                val outgoingCount = calls.count {
                    matchesType(it.call_type, "outgoing", "out", "geden", "2")
                }
                val missedCount = calls.count {
                    matchesType(it.call_type, "missed", "qaçırılan", "miss", "3")
                }

                binding.incomingCount.text = incoming.size.toString()
                binding.incomingDuration.text = "${incoming.sumOf { it.duration_sec } / 60} dəq"

                binding.callChart.incoming = incoming.size
                binding.callChart.outgoing = outgoingCount
                binding.callChart.missed = missedCount
                binding.callChart.invalidate()

                binding.legendIncoming.text = "● Gələn ${incoming.size}"
                binding.legendOutgoing.text = "● Gedən $outgoingCount"
                binding.legendMissed.text = "● Qaçırılan $missedCount"
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
