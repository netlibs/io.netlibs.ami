$version: "2"
namespace asterisk.ari

service ARI {
  version: "2023-10-01"
  resources: [
    Application
    Bridge
    Channel
    Playback
    Recording
  ]
}

