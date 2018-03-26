require("babel-polyfill");

require('./jsStyling')
window.sodium = require('libsodium-wrappers-sumo')
window.Hashes = require('jshashes');
const sharedb = require('sharedb/lib/client');

window.startDB = function(peer) {
  var socket = new WebSocket(peer);
  window.ShareDB = new sharedb.Connection(socket);
  return ShareDB;
}