package com.videodac.hls.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.videodac.hls.R
import com.videodac.hls.adapters.ChannelAdapter
import com.videodac.hls.helpers.StatusHelper.areChannelsInitialized
import com.videodac.hls.helpers.StatusHelper.channels
import com.videodac.hls.helpers.StatusHelper.ensNames
import com.videodac.hls.helpers.StatusHelper.threeBoxAvatarUris
import com.videodac.hls.helpers.StatusHelper.threeBoxNames
import com.videodac.hls.helpers.ThreeBoxHelper.threeBox
import com.videodac.hls.helpers.Utils
import com.videodac.hls.helpers.WebThreeHelper.web3
import kotlinx.android.synthetic.main.channels.*
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import org.json.JSONObject


class ChannelActivity : AppCompatActivity() {

    var adapter: ChannelAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.channels)

        // go to full screen
        Utils.goFullScreen(this)

        if(areChannelsInitialized){

            if(channels.isNullOrEmpty()) {

                hideChannels(true)

            } else {

                hideChannels(false)

                // get tht initial channel list
                initChannelList()
                getChannelENSNames()
                getThreeBoxProfiles()
            }

        } else {
            hideChannels(true)
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

    private  fun getChannelENSNames() {
        lifecycleScope.launch(Dispatchers.IO) {
            ensNames.clear()
            for ( it in channels) {

                if (Utils.isValidETHAddress(it)!!){

                    val ensName = Utils.resolveChannelENSName(it, web3!!)
                    if (ensName.isNotEmpty()) {
                        ensNames[it] = ensName
                    }
                }
            }

            withContext(Dispatchers.Main) {
                adapter!!.notifyDataSetChanged()
            }
        }
    }

    private fun getThreeBoxProfiles() {

        lifecycleScope.launch(Dispatchers.IO) {
            // clear the hashmaps first
            threeBoxAvatarUris.clear()
            threeBoxNames.clear()

            for (it in channels) {

                if (Utils.isValidETHAddress(it)!!){
                    val threeBoxRes = threeBox!!.getSpaceDetails(it)

                    if(threeBoxRes.isSuccessful) {
                        val threeBoxObj = JSONObject(threeBoxRes.body().toString())

                        if(!threeBoxObj.isNull(getString(R.string.three_box_name_key))){
                            val name = threeBoxObj.getString(getString(R.string.three_box_name_key))
                            threeBoxNames[it] = name

                            // then finally get the associated ipfs url hash
                            val imageHash = threeBoxObj.getString(getString(R.string.three_box_image_key))

                            // finally add it to the hashmap
                            threeBoxAvatarUris[it] = getString(R.string.ipfs_base_url) + imageHash
                        }

                        withContext(Dispatchers.Main) {
                            adapter!!.notifyDataSetChanged()
                        }
                    } else{
                        hideChannels(true)
                    }
                }
            }


        }

    }

    private fun hideChannels(hide: Boolean) {

        if (hide){
            channel_header.visibility = View.GONE
            channel_list.visibility = View.GONE
            no_channels.visibility = View.VISIBLE
        } else {
            channel_header.visibility = View.VISIBLE
            channel_list.visibility = View.VISIBLE
            no_channels.visibility = View.GONE
        }


    }


}