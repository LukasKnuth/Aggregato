# WebService Concept

## API

### Get Series/Episodes

- [x] If a series is not found in the database, schedule a crawl job for it 

### Calendar/Watchlist

> **Watchlist vs. Subscription**
> 
> *Watchlist*: List of **Episodes** not watched yet (updated with new entries from Interests)
>
> *Subscription*: List of **Series** a user is interested in (notification for new episodes, news, etc)

- [x] Add user authentication
- [x] A series can be added/removed/listed to a users subscriptions
- [x] Put new Episodes on the users watchlist when they air
- [x] Users can manually add **episodes and series** on their watchlist (adding a series adds all episodes)

### News

- [ ] Get news to a given Series (filter this by date? add limit?)

## Automatic Updating

### Database

- [ ] Check the Databases for new info every 24h
- [ ] Merge gathered information into existing information

### News

- [ ] Check news Feeds every 12h
- [x] Make the link the key, so we don't get doublets
