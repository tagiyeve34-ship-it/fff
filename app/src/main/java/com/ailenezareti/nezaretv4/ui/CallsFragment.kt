package com.ailenezareti.nezaretv4.ui
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ailenezareti.nezaretv4.Prefs
import com.ailenezareti.nezaretv4.R
import com.ailenezareti.nezaretv4.api.ApiClient
import com.ailenezareti.nezaretv4.databinding.*
import com.ailenezareti.nezaretv4.model.CallEntry
import kotlinx.coroutines.launch
import java.time.LocalDate
class CallsFragment:Fragment(){private var _b:FragmentCallsBinding?=null;private val b get()=_b!!;private var date=LocalDate.now();private var type="all";private val adapter=CallAdapter();override fun onCreateView(i:LayoutInflater,c:ViewGroup?,s:Bundle?)=FragmentCallsBinding.inflate(i,c,false).also{_b=it}.root
 override fun onViewCreated(v:View,s:Bundle?){b.list.layoutManager=LinearLayoutManager(requireContext());b.list.adapter=adapter;b.tabs.check(R.id.all);b.tabs.addOnButtonCheckedListener{_,id,checked->if(checked){type=when(id){R.id.incoming->"incoming";R.id.outgoing->"outgoing";R.id.missed->"missed";else->"all"};load()}};b.dateBtn.setOnClickListener{DatePickerDialog(requireContext(),{_,y,m,d->date=LocalDate.of(y,m+1,d);load()},date.year,date.monthValue-1,date.dayOfMonth).show()};load()}
 private fun load(){b.dateLabel.text=if(date==LocalDate.now())"Bu gün" else date.toString();val id=Prefs.child(requireContext());if(id<0)return;viewLifecycleOwner.lifecycleScope.launch{try{adapter.items=ApiClient.service(requireContext()).calls(id,date.toString(),date.toString(),type).body()?.calls.orEmpty();b.countLabel.text=adapter.items.size.toString();adapter.notifyDataSetChanged()}catch(_:Exception){}}}
 override fun onDestroyView(){super.onDestroyView();_b=null}
 class CallAdapter:RecyclerView.Adapter<CallAdapter.H>(){var items:List<CallEntry> = emptyList();class H(val b:ItemCallBinding):RecyclerView.ViewHolder(b.root);override fun onCreateViewHolder(p:ViewGroup,v:Int)=H(ItemCallBinding.inflate(LayoutInflater.from(p.context),p,false));override fun getItemCount()=items.size;override fun onBindViewHolder(h:H,p:Int){val x=items[p];h.b.name.text=x.contact_name?.takeIf{it.isNotBlank()}?:x.phone_number;h.b.number.text=x.phone_number;h.b.time.text=x.occurred_at.takeLast(8).take(5);h.b.duration.text=if(x.duration_sec>0)"${x.duration_sec/60} dəq ${x.duration_sec%60} san" else "Qaçırılan";val t=x.call_type.lowercase();h.b.icon.text=when{t.contains("out")->"↗";t.contains("miss")->"×";else->"↙"}}}}
