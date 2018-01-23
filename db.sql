CREATE TABLE event
(
  id        CHAR(32)     NOT NULL
    PRIMARY KEY,
  title     VARCHAR(100) NOT NULL,
  location  VARCHAR(100) NOT NULL,
  startTime CHAR(13)     NOT NULL,
  endTime   CHAR(13)     NOT NULL,
  creatorId CHAR(8)      NOT NULL
)
  ENGINE = InnoDB;

CREATE INDEX event_user_adminNo_fk
  ON event (creatorId);

CREATE TABLE eventdeletedinvite
(
  eventId CHAR(32) NOT NULL,
  adminNo CHAR(8)  NOT NULL,
  PRIMARY KEY (eventId, adminNo),
  CONSTRAINT eventdeletedinvite_event_id_fk
  FOREIGN KEY (eventId) REFERENCES event (id)
    ON DELETE CASCADE
)
  ENGINE = InnoDB;

CREATE TABLE eventgoing
(
  eventId CHAR(32) NOT NULL,
  adminNo CHAR(8)  NOT NULL,
  PRIMARY KEY (eventId, adminNo),
  CONSTRAINT eventgoing_event_id_fk
  FOREIGN KEY (eventId) REFERENCES event (id)
    ON DELETE CASCADE
)
  ENGINE = InnoDB;

CREATE TABLE eventhaventrespond
(
  eventId CHAR(32) NOT NULL,
  adminNo CHAR(8)  NOT NULL,
  PRIMARY KEY (eventId, adminNo),
  CONSTRAINT eventhaventrespond_event_id_fk
  FOREIGN KEY (eventId) REFERENCES event (id)
    ON DELETE CASCADE
)
  ENGINE = InnoDB;

CREATE TABLE eventnotgoing
(
  eventId CHAR(32) NOT NULL,
  adminNo CHAR(8)  NOT NULL,
  PRIMARY KEY (eventId, adminNo),
  CONSTRAINT eventnotgoing_event_id_fk
  FOREIGN KEY (eventId) REFERENCES event (id)
    ON DELETE CASCADE
)
  ENGINE = InnoDB;

CREATE TABLE filter
(
  adminNo   CHAR(8)         NOT NULL
    PRIMARY KEY,
  queries   INT DEFAULT '0' NOT NULL,
  cap       INT DEFAULT '5' NOT NULL,
  updatedAt CHAR(13)        NOT NULL
)
  ENGINE = InnoDB;

CREATE TABLE friend
(
  edgeId     CHAR(32) NOT NULL
    PRIMARY KEY,
  originNode CHAR(8)  NOT NULL,
  destNode   CHAR(8)  NOT NULL,
  CONSTRAINT friend_edgeId_uindex
  UNIQUE (edgeId)
)
  ENGINE = InnoDB;

CREATE INDEX originNode
  ON friend (originNode);

CREATE INDEX destNode
  ON friend (destNode);

CREATE TABLE friendrequest
(
  requestId CHAR(32) NOT NULL
    PRIMARY KEY,
  requestee CHAR(8)  NOT NULL,
  receiver  CHAR(8)  NOT NULL,
  CONSTRAINT friendRequest_requestId_uindex
  UNIQUE (requestId)
)
  ENGINE = InnoDB;

CREATE INDEX requestee
  ON friendrequest (requestee);

CREATE INDEX receiver
  ON friendrequest (receiver);

CREATE TABLE lesson
(
  id         CHAR(32)    NOT NULL
    PRIMARY KEY,
  moduleCode VARCHAR(15) NOT NULL,
  moduleName VARCHAR(50) NOT NULL,
  lessonType VARCHAR(10) NOT NULL,
  location   VARCHAR(20) NOT NULL,
  endTime    CHAR(13)    NULL,
  startTime  CHAR(13)    NOT NULL
)
  ENGINE = InnoDB;

CREATE TABLE lessonstudents
(
  lessonId CHAR(32) NOT NULL,
  adminNo  CHAR(8)  NOT NULL,
  PRIMARY KEY (lessonId, adminNo),
  CONSTRAINT lessonstudents_lesson_id_fk
  FOREIGN KEY (lessonId) REFERENCES lesson (id)
    ON DELETE CASCADE
)
  ENGINE = InnoDB;

CREATE TABLE user
(
  adminNo     CHAR(8)      NOT NULL
    PRIMARY KEY,
  username    VARCHAR(50)  NULL,
  displayName VARCHAR(100) NULL,
  CONSTRAINT user_adminNo_uindex
  UNIQUE (adminNo),
  CONSTRAINT user_username_uindex
  UNIQUE (username)
)
  ENGINE = InnoDB;

ALTER TABLE event
  ADD CONSTRAINT event_user_adminNo_fk
FOREIGN KEY (creatorId) REFERENCES user (adminNo)
  ON DELETE CASCADE;

ALTER TABLE filter
  ADD CONSTRAINT filter_user_adminNo_fk
FOREIGN KEY (adminNo) REFERENCES user (adminNo)
  ON DELETE CASCADE;

ALTER TABLE friend
  ADD CONSTRAINT friend_ibfk_1
FOREIGN KEY (originNode) REFERENCES user (adminNo);

ALTER TABLE friend
  ADD CONSTRAINT friend_ibfk_2
FOREIGN KEY (destNode) REFERENCES user (adminNo);

ALTER TABLE friendrequest
  ADD CONSTRAINT friendrequest_ibfk_1
FOREIGN KEY (requestee) REFERENCES user (adminNo);

ALTER TABLE friendrequest
  ADD CONSTRAINT friendrequest_ibfk_2
FOREIGN KEY (receiver) REFERENCES user (adminNo);

