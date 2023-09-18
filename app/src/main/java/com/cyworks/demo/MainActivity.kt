package com.cyworks.demo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.cyworks.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_layout)

        val depParent = findViewById<Button>(R.id.test_dep_parent)
        depParent.setOnClickListener { v ->
            val bundle = Bundle()
            bundle.putInt("LAUNCH_TYPE", LaunchType.DEP_PARENT.ordinal)
            open(bundle)
        }

        val depGlobal = findViewById<Button>(R.id.test_dep_golbal)
        depGlobal.setOnClickListener { v ->
            val bundle = Bundle()
            bundle.putInt("LAUNCH_TYPE", LaunchType.DEP_GLOBAL.ordinal)
            open(bundle)
        }

        val launchDialog = findViewById<Button>(R.id.test_launch_dialog)
        launchDialog.setOnClickListener { v ->
            val bundle = Bundle()
            bundle.putInt("LAUNCH_TYPE", LaunchType.LAUNCH_DIALOG.ordinal)
            open(bundle)
        }

        val installExtraComponent = findViewById<Button>(R.id.test_install_extra_component)
        installExtraComponent.setOnClickListener { v ->
            val bundle = Bundle()
            bundle.putInt("LAUNCH_TYPE", LaunchType.INSTALL_EXTRA_COMPONENT.ordinal)
            open(bundle)
        }

        val delayShowUI = findViewById<Button>(R.id.test_delay_show_sub_ui)
        delayShowUI.setOnClickListener { v ->
            val bundle = Bundle()
            bundle.putInt("LAUNCH_TYPE", LaunchType.DELAY_UI.ordinal)
            open(bundle)
        }

        val orientation = findViewById<Button>(R.id.test_orientation)
        orientation.setOnClickListener { v ->
            val bundle = Bundle()
            bundle.putInt("LAUNCH_TYPE", LaunchType.CHANGE_ORIENTATION.ordinal)
            open(bundle)
        }
    }

    private fun open(bundle: Bundle) {
        val intent = Intent()
        intent.putExtra("Mode", bundle)
        intent.setClass(this@MainActivity, TestActivity::class.java)
        this@MainActivity.startActivity(intent)
    }
}