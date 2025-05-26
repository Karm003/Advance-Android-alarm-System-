package com.unique.simplealarmclock.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.unique.simplealarmclock.R;
import com.unique.simplealarmclock.adapter.AlarmRecyclerViewAdapter;
import com.unique.simplealarmclock.model.Alarm;
import com.unique.simplealarmclock.util.QuoteManager;
import com.unique.simplealarmclock.viewmodel.AlarmListViewModel;
import com.unique.simplealarmclock.util.OnToggleAlarmListener;

import java.util.List;

public class AlarmsListFragment extends Fragment implements OnToggleAlarmListener {
    private AlarmRecyclerViewAdapter alarmRecyclerViewAdapter;
    private AlarmListViewModel alarmsListViewModel;
    private RecyclerView alarmsRecyclerView;
    private TextView quoteText;
    private FloatingActionButton addAlarm;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        alarmRecyclerViewAdapter = new AlarmRecyclerViewAdapter(this);
        alarmsListViewModel = ViewModelProviders.of(this).get(AlarmListViewModel.class);
        alarmsListViewModel.getAlarmsLiveData().observe(this, new Observer<List<Alarm>>() {
            @Override
            public void onChanged(List<Alarm> alarms) {
                if (alarms != null) {
                    alarmRecyclerViewAdapter.setAlarms(alarms);
                }
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_alarms_list, container, false);

        alarmsRecyclerView = view.findViewById(R.id.fragment_listalarms_recylerView);
        alarmsRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        alarmsRecyclerView.setAdapter(alarmRecyclerViewAdapter);

        addAlarm = view.findViewById(R.id.fragment_listalarms_addAlarm);
        addAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(v).navigate(R.id.action_alarmsListFragment_to_createAlarmFragment);
            }
        });

        // Setup quote display
        quoteText = view.findViewById(R.id.quote_text);
        updateQuote();

        return view;
    }

    private void updateQuote() {
        if (quoteText != null) {
            String quote = QuoteManager.getRandomQuote();
            quoteText.setText(quote);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Update quote when returning to this fragment
        updateQuote();
    }

    @Override
    public void onToggle(Alarm alarm) {
        if (alarm.isStarted()) {
            alarm.cancelAlarm(getContext());
            alarmsListViewModel.update(alarm);
        } else {
            alarm.schedule(getContext());
            alarmsListViewModel.update(alarm);
        }
    }

    @Override
    public void onDelete(Alarm alarm) {
        if (alarm.isStarted())
            alarm.cancelAlarm(getContext());
        alarmsListViewModel.delete(alarm.getAlarmId());
    }

    @Override
    public void onItemClick(Alarm alarm,View view) {
        if (alarm.isStarted())
            alarm.cancelAlarm(getContext());
        Bundle args = new Bundle();
        args.putSerializable(getString(R.string.arg_alarm_obj),alarm);
        Navigation.findNavController(view).navigate(R.id.action_alarmsListFragment_to_createAlarmFragment,args);
    }
}