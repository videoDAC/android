//
//  ViewController.swift
//  Video Dac
//
//  Created by Mulili Nzuki on 01/04/20.
//  Copyright © 2020 mul1sh. All rights reserved.
//

import UIKit
import AVFoundation
import AVKit

class ViewController: UIViewController {
    
    let avPlayerViewController = AVPlayerViewController()
    var avPlayer: AVPlayer?
    let STREAM_URL = "http://159.100.251.158:8935/stream/0xdac817294c0c87ca4fa1895ef4b972eade99f2fd.m3u8"

    override func viewDidLoad() {
        super.viewDidLoad()
        
        guard let url = URL(string: STREAM_URL) else { return }
        let asset = AVAsset(url: url)
        let playerItem = AVPlayerItem(asset: asset)
        self.avPlayer = AVPlayer(playerItem: playerItem)
        avPlayerViewController.player = self.avPlayer
        self.addChild(avPlayerViewController)
        self.view.addSubview(avPlayerViewController.view)

        self.avPlayerViewController.didMove(toParent: self)
        self.avPlayerViewController.player?.play()

        self
        .avPlayerViewController
        .player?
        .addObserver(self, forKeyPath: "currentItem.presentationSize", options: [.initial, .new], context: nil)

        let gesture = UITapGestureRecognizer(target: self, action:  #selector (self.closePlayer (_:)))
        self.view.addGestureRecognizer(gesture)
    }
    
    
    @objc func closePlayer(_ sender:UITapGestureRecognizer){
         self.avPlayer?.pause()
         self.avPlayerViewController.removeFromParent()
    }

    override public func observeValue(forKeyPath keyPath: String?, of object: Any?, change: [NSKeyValueChangeKey: Any]?, context: UnsafeMutableRawPointer?) {

        if keyPath == "currentItem.presentationSize" {

            let videoSize  = change?[.newKey] as? NSValue
            guard let width  = videoSize?.cgSizeValue.width else { return }
            guard let height = videoSize?.cgSizeValue.height else { return }

            if( height.isLess(than: width)) {
                let value = UIInterfaceOrientation.landscapeRight.rawValue
                UIDevice.current.setValue(value, forKey: "orientation")
            }

            if( width.isLess(than: height)) {
                let value = UIInterfaceOrientation.portrait
                UIDevice.current.setValue(value, forKey: "orientation")
            }
        }
        else {
            super.observeValue(forKeyPath: keyPath, of: object, change: change, context: context)
            return
        }

    }

}
