package com.dp.logcatapp.fragments.settings.dialogs

import android.app.Application
import android.app.Dialog
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Environment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.dp.logcatapp.R
import com.dp.logcatapp.fragments.base.BaseDialogFragment
import com.dp.logcatapp.fragments.settings.SettingsFragment
import com.dp.logcatapp.util.PreferenceKeys
import com.dp.logcatapp.util.getDefaultSharedPreferences
import com.dp.logcatapp.util.inflateLayout
import java.io.File


class FolderChooserDialogFragment : BaseDialogFragment(), View.OnClickListener {

    companion object {
        val TAG = FolderChooserDialogFragment::class.qualifiedName
    }

    private val recyclerViewAdapter = MyRecyclerViewAdapter(this)
    private lateinit var layoutManager: LinearLayoutManager
    private lateinit var viewModel: MyViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProviders.of(this)
                .get(MyViewModel::class.java)
        viewModel.files.observe(this, Observer<List<FileHolder>> {
            if (it != null) {
                recyclerViewAdapter.setData(it)
            }
        })
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = inflateLayout(R.layout.folder_chooser)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.recyclerView)

        layoutManager = LinearLayoutManager(activity!!)

        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = recyclerViewAdapter

        return AlertDialog.Builder(activity!!)
                .setTitle("Select a folder")
                .setView(rootView)
                .setPositiveButton(getString(R.string.select), { _, _ ->
                    val folder = if (recyclerViewAdapter.itemCount > 0) {
                        val fileHolder = recyclerViewAdapter[0]
                        if (fileHolder.isParent) {
                            fileHolder.file
                        } else {
                            null
                        }
                    } else {
                        null
                    }

                    (targetFragment as SettingsFragment).setupCustomSaveLocationPreLollipop(folder)
                })
                .setNegativeButton(android.R.string.cancel, { _, _ ->
                    dismiss()
                })
                .create()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.folderChooserListItem -> {
                val pos = layoutManager.getPosition(v)
                if (pos != RecyclerView.NO_POSITION) {
                    val fileHolder = recyclerViewAdapter[pos]
                    if (fileHolder.isParent) {
                        if (fileHolder.file.parentFile != null) {
                            viewModel.update(fileHolder.file.parentFile)
                        }
                    } else {
                        if (fileHolder.file.isDirectory) {
                            viewModel.update(fileHolder.file)
                        }
                    }
                }
            }
        }
    }
}


internal class MyViewModel(application: Application) : AndroidViewModel(application) {
    val files = MutableLiveData<List<FileHolder>>()

    init {
        val path = application.getDefaultSharedPreferences().getString(
                PreferenceKeys.Logcat.KEY_SAVE_LOCATION,
                ""
        )

        val file = if (path.isEmpty()) {
            Environment.getExternalStorageDirectory()
        } else {
            File(path)
        }

        update(file)
    }

    fun update(file: File) {
        val files = mutableListOf<FileHolder>()

        if (file.parentFile != null) {
            files.add(FileHolder(file, isParent = true))
        }

        file.listFiles()
                ?.map { FileHolder(it) }
                ?.sortedBy { it.file.name }
                ?.forEach({ files.add(it) })

        this.files.value = files
    }
}

internal data class FileHolder(val file: File, val isParent: Boolean = false)

private class MyRecyclerViewAdapter(private val onClickListener: View.OnClickListener) :
        RecyclerView.Adapter<MyRecyclerViewAdapter.MyViewHolder>() {

    private var data = listOf<FileHolder>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.folder_chooser_list_item,
                parent, false)
        view.setOnClickListener(onClickListener)
        return MyViewHolder(view)
    }

    override fun getItemCount() = data.size

    fun setData(items: List<FileHolder>) {
        data = items
        notifyDataSetChanged()
    }

    operator fun get(index: Int) = data[index]

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val fileHolder = data[position]
        if (fileHolder.isParent) {
            holder.fileName.text = ".."
        } else {
            holder.fileName.text = fileHolder.file.name
        }
        if (fileHolder.file.isDirectory) {
            holder.fileIcon.setImageResource(R.drawable.ic_folder_grey_300_24dp)
        } else {
            holder.fileIcon.setImageResource(R.drawable.ic_insert_drive_file_grey_300_24dp)
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val fileIcon: ImageView = itemView.findViewById(R.id.fileIcon)
        val fileName: TextView = itemView.findViewById(R.id.fileName)
    }
}