package com.bignerdranch.android.criminalintent

import android.os.Bundle
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

abstract class SingleFragmentActivity : AppCompatActivity() {

    protected abstract fun createFragment(): Fragment

    @LayoutRes
    protected open fun getLayoutResId(): Int {
        return R.layout.activity_fragment
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())

        val fragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

        if (fragment == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.fragment_container, createFragment())
                .commit()
        }
    }
}
