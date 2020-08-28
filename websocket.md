# websocket ideas

- fetch jda entities over WS
- check permissions
- check if bot in guild
- creating role hashes

#### Authorising
Request (from-bot):
```json
{
  "t": "IDENTIFY",
  "token": "auth token for api"
}
```

#### Internal data exchange
Request (from-bot):
```json
{
  "t": "IDENTIFY",
  "success": true,
  "message": "Only present if success is false, gives info about the error"
}
```
Response (to-bot):
```json
{
  "t": "ROLES_PUT_HASH",
  "data": {
    "guild_id": "1321513153",
    "hash": "blablabla",
    "success": true 
  }
}
```

#### invalidating guild settings and updating
Request (to-bot):
```json
{
  "t": "GUILD_SETTINGS",
  "remove": [
    "array of guild ids to forget the settings for"
  ],
  "update": [
    "array of guild ids to update the settings for"
  ],
  "add": [
    "array of guild ids to fetch the settings for"
  ]
}
```

#### getting command info
Request (to-bot):
```json
{
  "t": "RETRIEVE",
  "identifier": "Identifier for finding the data we requested",
  "commands": {}
}
```
Response (from-bot):
```json
{
  "t": "RETRIEVE",
  "identifier": "Identifier for finding the data we requested",
  "commands": [
    {
      "name": "etc..."
    }
  ]
}
```

#### getting guild info
All fields are optional

Request (to-bot):
```json
{
  "t": "RETRIEVE",
  "identifier": "Identifier for finding the data we requested",
  "guilds": [
    "list of guild ids"
  ],
  "guild_member_info": {
    "guild_id": "13456",
    "member_id": "5464654684"
  }
}
```
Response (from-bot):
```json
{
  "t": "RETRIEVE",
  "identifier": "Identifier for finding the data we requested",
  "guilds": [
    {
      "guild_id": "13456",
      "member_count": 999, // or -1 for not in server
      "text_channels": [
        // JDA text channel object
      ],
      "voice_channels": [
        // JDA voice channel object
      ],
      "roles": [
        // JDA role object
      ]
    }
  ],
   "guild_member_info": {
      "guild_id": "13456",
      "member_id": "5464654684",
      "member": {
        // JDA member object
      }
   }
}
```
