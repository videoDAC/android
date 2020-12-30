package com.videodac.hls.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import com.videodac.hls.R
import com.videodac.hls.adapters.ChannelAdapter
import com.videodac.hls.databinding.ChannelsBinding
import com.videodac.hls.helpers.StatusHelper.channels
import com.videodac.hls.helpers.StatusHelper.ensNames
import com.videodac.hls.helpers.StatusHelper.threeBoxAvatarUris
import com.videodac.hls.helpers.StatusHelper.threeBoxNames
import com.videodac.hls.helpers.ThreeBoxHelper.threeBox
import com.videodac.hls.helpers.Utils
import com.videodac.hls.helpers.WebThreeHelper.web3

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONException
import org.json.JSONObject

class ChannelActivity : AppCompatActivity() {

    var adapter: ChannelAdapter? = null
    private val TAG = "CHANNEL_ACTIVITY"

    // view binding
    private lateinit var binding: ChannelsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // inflate binding
        binding = ChannelsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // go to full screen
        Utils.goFullScreen(this)

        if(channels.isNullOrEmpty()) {

            binding.channelHeader.visibility = View.GONE
            binding.channelList.visibility = View.GONE
            binding.noChannels.visibility = View.VISIBLE

        } else {

            binding.channelHeader.visibility = View.VISIBLE
            binding.channelList.visibility = View.VISIBLE
            binding.noChannels.visibility = View.GONE

            // get the initial channel list
            initChannelList()

            // then fetch the ENS names and 3box profiles
            lifecycleScope.launch(Dispatchers.IO) {
                getThreeBoxProfiles()
                getChannelENSNames()
            }
        }

    }

    private fun initChannelList() {
        val mRecyclerView = findViewById<RecyclerView>(R.id.channel_list)
        mRecyclerView.setHasFixedSize(true)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))

        adapter = ChannelAdapter(channels, this)
        mRecyclerView.adapter = adapter
        adapter!!.notifyDataSetChanged()
    }

    private suspend fun getChannelENSNames() {
        // clear the hashmap first
        ensNames.clear()

        for ( it in channels) {

            if (Utils.isValidETHAddress(it)){

                val ensName = Utils.resolveChannelENSName(it, web3!!)

                if (ensName.isNotEmpty()) ensNames[it] = ensName
            }
        }

        withContext(Dispatchers.Main) {
            adapter!!.notifyDataSetChanged()
        }
    }

    private suspend fun getThreeBoxProfiles() {

        // clear the hashmaps first
        threeBoxAvatarUris.clear()
        threeBoxNames.clear()

        for (it in channels) {

            if (Utils.isValidETHAddress(it)){
                val threeBoxRes = threeBox!!.getSpaceDetails(it)

                if(threeBoxRes.isSuccessful) {

                    try {
                        val threeBoxObj = JSONObject(threeBoxRes.body().toString())

                        val name = threeBoxObj.getString(getString(R.string.three_box_name_key))
                        threeBoxNames[it] = name

                        // then finally get the associated ipfs url hash
                        val imageHash = threeBoxObj.getString("image")

                        // finally add it to the hashmap
                        threeBoxAvatarUris[it] = getString(R.string.ipfs_base_url) + imageHash

                    }
                    catch (je: JSONException){
                        Log.d(TAG, je.localizedMessage!!)
                    }


                }
            }
        }

        withContext(Dispatchers.Main) {
            adapter!!.notifyDataSetChanged()
        }


    }




}