package com.hunterwilhelm.offsetclocks

import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.util.*
import kotlin.collections.ArrayList


class MainActivity : AppCompatActivity() {
    private lateinit var timerTask: TimerTask
    private var disableButtons: Boolean = false
    private lateinit var myAdapter: MainClockListAdapter
    private lateinit var sPrefs: SharedPreferences
    private lateinit var listView: NonScrollListView

    private var setting24Hour: Boolean = false
    private var settingShowDay: Boolean = false
    private lateinit var viewModel: SharedViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.main_toolbar)
        setSupportActionBar(toolbar)

        listView = findViewById<NonScrollListView>(R.id.nonscroll_list)
        myAdapter = MainClockListAdapter(this, R.layout.row_main, ArrayList<ClockModel>())
        listView.adapter = myAdapter

        viewModel = ViewModelProvider(this).get(SharedViewModel::class.java)
        sPrefs = applicationContext.getSharedPreferences(
            getString(R.string.preference_key_clock_storage),
            MODE_PRIVATE
        )

        // setup for timer
        registerTimers()
        registerListeners()
    }

    private fun registerTimers() {
        timerTask = object : TimerTask() {
            override fun run() {
                try {
                    myAdapter.update()
                } catch (e: ConcurrentModificationException) {
                    e.printStackTrace()
                }
            }
        }
        Timer().scheduleAtFixedRate(timerTask, 0, 50)
    }

    private fun registerListeners() {
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener {
            if (!disableButtons) {
                editClock(null, null)
            }
            disableButtons = true
        }
        listView.setOnItemClickListener { _: AdapterView<*>, _: View, i: Int, _: Long ->
            if (!disableButtons) {
                editClock(myAdapter.getItem(i), i)
            }
            disableButtons = true
        }
    }

    private fun loadData() {
        val clockKey = getString(R.string.preference_key_clocks)
        myAdapter.clear()
        myAdapter.addAll(Utils.getClocksFromStorage(sPrefs, clockKey))
        if (myAdapter.items.count() == 0) {
            val defaultClock = ClockModel(getString(R.string.defualt_clock_name), "", 0)
            myAdapter.add(defaultClock)
            // we only need to update the storage when we have added something
            Utils.putClocksIntoStorage(sPrefs, clockKey, myAdapter.items)
        }
        myAdapter.notifyDataSetChanged()

        setting24Hour =
            Utils.getBooleanPreference(sPrefs, getString(R.string.preference_key_setting_24_hour))
        settingShowDay =
            Utils.getBooleanPreference(sPrefs, getString(R.string.preference_key_setting_show_day))
        myAdapter.updateSettings(setting24Hour, settingShowDay)
        viewModel.sendShowDayPreference(settingShowDay)
    }

    private fun editClock(clock: ClockModel?, index: Int?) {
        val intent = Intent(this, EditActivity::class.java)
        if (clock != null && index != null) {
            intent.putExtra(IntentExtraConstants.CLOCK_DELAY.name, clock.delay)
            intent.putExtra(IntentExtraConstants.CLOCK_INDEX.name, index)
            intent.putExtra(IntentExtraConstants.CLOCK_NAME.name, clock.Name)
            intent.putExtra(IntentExtraConstants.SETTING_24_HOUR.name, setting24Hour)
        }
        startActivity(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            // Apply activity transition
            overridePendingTransition(
                R.anim.slide_in_right,
                R.anim.nothing
            )
        }
    }

    override fun onResume() {
        super.onResume()
        loadData()
        disableButtons = false
    }

    override fun onDestroy() {
        timerTask.cancel()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        menu.findItem(R.id.action_show_day).isChecked = settingShowDay
        menu.findItem(R.id.action_24_hour_time).isChecked = setting24Hour
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if (item.itemId == R.id.action_24_hour_time) {
            item.isChecked = !item.isChecked
            setting24Hour = item.isChecked
            val key = getString(R.string.preference_key_setting_24_hour)
            Utils.setBooleanPreference(sPrefs, key, setting24Hour)
        } else if (item.itemId == R.id.action_show_day) {
            item.isChecked = !item.isChecked
            settingShowDay = item.isChecked
            val key = getString(R.string.preference_key_setting_show_day)
            Utils.setBooleanPreference(sPrefs, key, item.isChecked)
        }
        myAdapter.updateSettings(setting24Hour, settingShowDay)
        return super.onOptionsItemSelected(item)
    }
}