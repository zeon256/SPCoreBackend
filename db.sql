create table event
(
  id char(32) not null
    primary key,
  title varchar(100) not null,
  location varchar(100) not null,
  startTime char(13) not null,
  endTime char(13) not null,
  creatorId char(8) not null
)
  engine=InnoDB
;

create index event_user_adminNo_fk
  on event (creatorId)
;

create table eventdeletedinvite
(
  eventId char(32) not null,
  adminNo char(8) not null,
  primary key (eventId, adminNo),
  constraint eventdeletedinvite_event_id_fk
  foreign key (eventId) references event (id)
    on delete cascade
)
  engine=InnoDB
;

create table eventgoing
(
  eventId char(32) not null,
  adminNo char(8) not null,
  primary key (eventId, adminNo),
  constraint eventgoing_event_id_fk
  foreign key (eventId) references event (id)
    on delete cascade
)
  engine=InnoDB
;

create table eventhaventrespond
(
  eventId char(32) not null,
  adminNo char(8) not null,
  primary key (eventId, adminNo),
  constraint eventhaventrespond_event_id_fk
  foreign key (eventId) references event (id)
    on delete cascade
)
  engine=InnoDB
;

create table eventnotgoing
(
  eventId char(32) not null,
  adminNo char(8) not null,
  primary key (eventId, adminNo),
  constraint eventnotgoing_event_id_fk
  foreign key (eventId) references event (id)
    on delete cascade
)
  engine=InnoDB
;

create table friend
(
  edgeId char(32) not null
    primary key,
  originNode char(8) not null,
  destNode char(8) not null,
  constraint friend_edgeId_uindex
  unique (edgeId)
)
  engine=InnoDB
;

create index originNode
  on friend (originNode)
;

create index destNode
  on friend (destNode)
;

create table friendrequest
(
  requestId char(32) not null
    primary key,
  requestee char(8) not null,
  receiver char(8) not null,
  constraint friendRequest_requestId_uindex
  unique (requestId)
)
  engine=InnoDB
;

create index requestee
  on friendrequest (requestee)
;

create index receiver
  on friendrequest (receiver)
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
  engine=InnoDB
;

create table lessonstudents
(
  lessonId char(32) not null,
  adminNo char(8) not null,
  primary key (lessonId, adminNo),
  constraint lessonstudents_lesson_id_fk
  foreign key (lessonId) references lesson (id)
    on update cascade on delete cascade
)
  engine=InnoDB
;

create index lessonstudents_user_adminNo_fk
  on lessonstudents (adminNo)
;

create table user
(
  adminNo char(8) not null
    primary key,
  username varchar(50) null,
  displayName varchar(100) null,
  constraint user_username_uindex
  unique (username)
)
  engine=InnoDB
;

alter table event
  add constraint event_user_adminNo_fk
foreign key (creatorId) references user (adminNo)
  on update cascade on delete cascade
;

alter table friend
  add constraint friend_ibfk_1
foreign key (originNode) references user (adminNo)
;

alter table friend
  add constraint friend_ibfk_2
foreign key (destNode) references user (adminNo)
;

alter table friendrequest
  add constraint friendrequest_ibfk_1
foreign key (requestee) references user (adminNo)
;

alter table friendrequest
  add constraint friendrequest_ibfk_2
foreign key (receiver) references user (adminNo)
;

alter table lessonstudents
  add constraint lessonstudents_user_adminNo_fk
foreign key (adminNo) references user (adminNo)
  on update cascade on delete cascade
;

create table userdevice
(
  adminNo char(8) not null,
  deviceId varchar(1000) not null,
  primary key (adminNo, deviceId),
  constraint userdevice_user_adminNo_fk
  foreign key (adminNo) references user (adminNo)
    on update cascade on delete cascade
)
  engine=InnoDB
;

