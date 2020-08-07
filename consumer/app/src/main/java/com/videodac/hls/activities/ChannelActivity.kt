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
import com.videodac.hls.helpers.StatusHelper.channels
import com.videodac.hls.helpers.StatusHelper.threeBoxAvatarUris
import com.videodac.hls.helpers.StatusHelper.threeBoxNames
import com.videodac.hls.helpers.ThreeBoxHelper.threeBox
import com.videodac.hls.helpers.Utils
import com.videodac.hls.helpers.WebThreeHelper.web3
import kotlinx.android.synthetic.main.channels.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
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


        if(channels.isNullOrEmpty()) {

            channel_header.visibility = View.GONE
            channel_list.visibility = View.GONE

            no_channels.visibility = View.VISIBLE

        } else {

            channel_header.visibility = View.VISIBLE
            channel_list.visibility = View.VISIBLE

            no_channels.visibility = View.GONE

            // get tht initial channel list
            initChannelList()
            getChannelENSNames()
            getThreeBoxProfiles()
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
            for ((index, it) in channels.withIndex()) {

                if (Utils.isValidETHAddress(it)!!){

                    val ensName = Utils.resolveChannelENSName(it, web3!!)
                    if (ensName.isNotEmpty()) {
                        channels[index] = ensName
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
                    val threeBoxRes = threeBox!!.getProfile(it)

                    if(threeBoxRes.isSuccessful) {
                        val threeBoxObj = JSONObject(threeBoxRes.body().toString())

                        // get the 3box profile name
                        val name = threeBoxObj.getString(getString(R.string.three_box_name_key))

                        // add it to the hashmap with the
                        threeBoxNames[it] = name

                        // then try to retrieve the image
                        val imageArray = threeBoxObj.getJSONArray(getString(R.string.three_box_image_key))

                        if (imageArray.length() > 0 ){
                            // get the first image object
                            val imageObj = JSONObject(imageArray[0].toString())

                            // then get the associated content url object
                            val imageHashObj = JSONObject(imageObj.getString(getString(R.string.three_box_content_url_key)))

                            // then finally get the associated ipfs url hash
                            val imageHash = imageHashObj.getString(getString(R.string.three_box_content_hash))

                            // finally add it to the hashmap
                            threeBoxAvatarUris[it] = getString(R.string.ipfs_base_url) + imageHash

                        }

                    }
                }
            }

            withContext(Dispatchers.Main) {
                adapter!!.notifyDataSetChanged()
            }
        }

    }

}