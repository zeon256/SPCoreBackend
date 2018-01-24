create table user
(
  adminNo char(8) not null
    primary key,
  username varchar(50) null,
  displayName varchar(100) null,
  constraint user_adminNo_uindex
  unique (adminNo),
  constraint user_username_uindex
  unique (username)
);

create table event
(
id char(32) not null
primary key,
title varchar(100) not null,
location varchar(100) not null,
startTime char(13) not null,
endTime char(13) not null,
creatorId char(8) not null,
constraint event_user_adminNo_fk
foreign key (creatorId) references spcore.user (adminNo)
on delete cascade
);

create index event_user_adminNo_fk
on event (creatorId);

create table eventdeletedinvite
(
eventId char(32) not null,
adminNo char(8) not null,
primary key (eventId, adminNo),
constraint eventdeletedinvite_event_id_fk
foreign key (eventId) references spcore.event (id)
on delete cascade
)
;

create table eventgoing
(
eventId char(32) not null,
adminNo char(8) not null,
primary key (eventId, adminNo),
constraint eventgoing_event_id_fk
foreign key (eventId) references spcore.event (id)
on delete cascade
)
;

create table eventhaventrespond
(
eventId char(32) not null,
adminNo char(8) not null,
primary key (eventId, adminNo),
constraint eventhaventrespond_event_id_fk
foreign key (eventId) references spcore.event (id)
on delete cascade
)
;

create table eventnotgoing
(
eventId char(32) not null,
adminNo char(8) not null,
primary key (eventId, adminNo),
constraint eventnotgoing_event_id_fk
foreign key (eventId) references spcore.event (id)
on delete cascade
)
;

create table filter
(
adminNo char(8) not null
primary key,
queries int default '0' not null,
cap int default '5' not null,
updatedAt char(13) not null,
constraint filter_user_adminNo_fk
foreign key (adminNo) references spcore.user (adminNo)
on delete cascade
)
;

create table friend
(
edgeId char(32) not null
primary key,
originNode char(8) not null,
destNode char(8) not null,
constraint friend_edgeId_uindex
unique (edgeId),
constraint friend_ibfk_1
foreign key (originNode) references spcore.user (adminNo),
constraint friend_ibfk_2
foreign key (destNode) references spcore.user (adminNo)
)
;

create index destNode
on friend (destNode)
;

create index originNode
on friend (originNode)
;

create table friendrequest
(
requestId char(32) not null
primary key,
requestee char(8) not null,
receiver char(8) not null,
constraint friendRequest_requestId_uindex
unique (requestId),
constraint friendrequest_ibfk_1
foreign key (requestee) references spcore.user (adminNo),
constraint friendrequest_ibfk_2
foreign key (receiver) references spcore.user (adminNo)
)
;

create index receiver
on friendrequest (receiver)
;

create index requestee
on friendrequest (requestee)
;

create table lesson
(
id char(32) not null
primary key,
moduleCode varchar(15) not null,
moduleName varchar(50) not null,
lessonType varchar(10) not null,
location varchar(20) not null,
endTime char(13) null,
startTime char(13) not null
)
;

create table lessonstudents
(
lessonId char(32) not null,
adminNo char(8) not null,
primary key (lessonId, adminNo)
)
;


