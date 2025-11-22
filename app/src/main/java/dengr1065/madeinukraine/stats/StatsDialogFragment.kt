package dengr1065.madeinukraine.stats

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dengr1065.madeinukraine.MainViewModel
import dengr1065.madeinukraine.R
import dengr1065.madeinukraine.databinding.FragmentStatsBinding
import kotlinx.coroutines.launch

class StatsDialogFragment : DialogFragment() {
    
    private val viewModel by activityViewModels<MainViewModel>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        Log.d(TAG, "onCreateDialog: called")
        val binding = FragmentStatsBinding.inflate(layoutInflater)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.databaseCount.collect {
                    binding.databaseCount.text = it.toString()
                }
            }
        }

        return AlertDialog.Builder(binding.root.context)
            .setTitle(R.string.stats_title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }
    
    companion object {
        private const val TAG = "StatsDialogFragment"
    }
}