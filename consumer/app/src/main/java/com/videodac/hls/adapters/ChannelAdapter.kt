package com.videodac.hls.adapters

import android.content.Intent
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

import com.github.ybq.android.spinkit.SpinKitView

import com.videodac.hls.R
import com.videodac.hls.activities.VideoActivity
import com.videodac.hls.helpers.Constants.CHANNEL_ADDRESS
import com.videodac.hls.helpers.StatusHelper.ensNames
import com.videodac.hls.helpers.StatusHelper.threeBoxAvatarUris
import com.videodac.hls.helpers.StatusHelper.threeBoxNames
import com.videodac.hls.helpers.Utils
import com.videodac.hls.ui.BlockiesIdenticon

import org.web3j.crypto.Hash
import org.web3j.utils.Numeric
import java.util.*

class ChannelAdapter(
    private val channels: MutableList<String>,
    private val activity: AppCompatActivity
) : RecyclerView.Adapter<ChannelAdapter.ViewHolder>() {

    override fun getItemCount() = channels.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // only load valid ETH addresses
        val channel = channels[position]

        if(Utils.isValidETHAddress(channel)) {

             holder.realChannelAddress.text = channel

             val channelName = threeBoxNames[channel]
             val ensName = ensNames[channel]

             if (!channelName.isNullOrEmpty()) {
                holder.channelAddress.text = channelName

                val userImageUri = threeBoxAvatarUris[channel]

                if (!userImageUri.isNullOrEmpty()) {

                    // then hide the identicon
                    holder.channelIdenticon.visibility = View.GONE
                    holder.imgLoading.visibility = View.VISIBLE

                    Glide
                        .with(activity)
                        .load(userImageUri)
                        .centerCrop()
                        .listener(object : RequestListener<Drawable> {
                            override fun onLoadFailed(
                                e: GlideException?,
                                model: Any,
                                target: Target<Drawable>,
                                isFirstResource: Boolean
                            ): Boolean {
                                holder.imgLoading.visibility = View.GONE
                                holder.channelIdenticon.visibility = View.VISIBLE
                                return false
                            }

                            override fun onResourceReady(
                                resource: Drawable,
                                model: Any,
                                target: Target<Drawable>,
                                dataSource: DataSource,
                                isFirstResource: Boolean
                            ): Boolean {
                                holder.imgLoading.visibility = View.GONE
                                return false
                            }

                        }).into(holder.channelIcon).clearOnDetach()
                }

            } else if(!ensName.isNullOrEmpty()) {
                 holder.channelAddress.text = ensName

                 // then show the identicon
                 holder.channelIdenticon.visibility = View.VISIBLE

                 // and hide the image icon
                 holder.channelIcon.visibility = View.GONE

                 val params = holder.channelAddress.layoutParams as RelativeLayout.LayoutParams
                 params.setMargins(260, 0, 0, 0)

                 holder.channelAddress.layoutParams = params

                 val hash = Numeric.toBigInt(
                     Hash.sha3(
                         channel.toLowerCase(Locale.ROOT).toByteArray()
                     )
                 )
                 holder.channelIdenticon.setAddress(channel)
             }
            else{
                holder.channelAddress.text = channel

                // then show the identicon
                holder.channelIdenticon.visibility = View.VISIBLE
                val hash = Numeric.toBigInt(
                    Hash.sha3(
                        channel.toLowerCase(Locale.ROOT).toByteArray()
                    )
                )
                 holder.channelIdenticon.setAddress(channel)
            }

        }


    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelAdapter.ViewHolder {
        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.channel_item,
                parent,
                false
            )
        )
    }

    inner class ViewHolder(v: View) : RecyclerView.ViewHolder(v),  View.OnClickListener {

        val channelAddress = v.findViewById(R.id.channel_address) as TextView
        val realChannelAddress = v.findViewById(R.id.real_channel_address) as TextView
        val channelIcon = v.findViewById(R.id.channel_icon) as ImageView
        val channelIdenticon = v.findViewById(R.id.channel_identicon) as BlockiesIdenticon
        val imgLoading = v.findViewById(R.id.img_loader) as SpinKitView

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View?) {

            if(Utils.isValidETHAddress(realChannelAddress.text.toString())) {
                activity.startActivity(
                    Intent(activity, VideoActivity::class.java).putExtra(
                        CHANNEL_ADDRESS,
                        realChannelAddress.text.toString()
                    )
                )
                Utils.closeActivity(activity, null, null)
            }
        }
    }
}