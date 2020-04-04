create extension if not exists "uuid-ossp";

create table users
(
	id uuid not null
		constraint users_pkey
			primary key,
	name varchar(50),
	telegram_id bigint
		constraint users_telegram_id_unique
			unique,
	email varchar(50)
		constraint users_email_unique
			unique,
	first_name varchar(50),
	last_name varchar(50),
	language varchar(5),
	otp varchar(50),
	created timestamp
);

create type valuetypeenum as enum ('Mood', 'EndDate');

create table tags
(
	id uuid not null
		constraint tags_pkey
			primary key,
	name varchar(50) not null
		constraint tags_name_unique
			unique
);

create table habits
(
	id uuid not null
		constraint habits_pkey
			primary key,
	name varchar(50) not null,
	"user" uuid not null
		constraint fk_habits_user_id
			references users
				on update restrict on delete restrict,
	number_of_repetitions integer not null,
	period integer not null,
	quote varchar(500),
	bad boolean default false,
	"startFrom" timestamp,
	created timestamp
);

create table actions
(
	id uuid not null
		constraint actions_pkey
			primary key,
	description varchar(50) not null,
	"user" uuid not null
		constraint fk_actions_user_id
			references users
				on update restrict on delete restrict,
	habit uuid
		constraint fk_actions_habit_id
			references habits
				on update restrict on delete cascade,
	created timestamp
);

create table habit_tags
(
	"habitId" uuid not null
		constraint fk_habit_tags_habitid_id
			references habits
				on update restrict on delete cascade,
	"tagId" uuid not null
		constraint fk_habit_tags_tagid_id
			references tags
				on update restrict on delete cascade,
	constraint pk_habit_tags
		primary key ("habitId", "tagId")
);

create table action_tags
(
	"actionId" uuid not null
		constraint fk_action_tags_actionid_id
			references actions
				on update restrict on delete cascade,
	"tagId" uuid not null
		constraint fk_action_tags_tagid_id
			references tags
				on update restrict on delete cascade,
	constraint pk_action_tags
		primary key ("actionId", "tagId")
);

create table user_tags
(
	"userId" uuid not null
		constraint fk_user_tags_userid_id
			references users
				on update restrict on delete cascade,
	"tagId" uuid not null
		constraint fk_user_tags_tagid_id
			references tags
				on update restrict on delete cascade,
	constraint pk_user_tags
		primary key ("userId", "tagId")
);

create table values
(
	id uuid not null
		constraint values_pkey
			primary key,
	"actionId" uuid not null
		constraint fk_values_actionid_id
			references actions
				on update restrict on delete cascade,
	type valuetypeenum not null,
	name varchar(200),
	value varchar(200)
);

create table values_templates
(
	id uuid not null
		constraint values_templates_pkey
			primary key,
	"habitId" uuid not null
		constraint fk_values_templates_habitid_id
			references habits
				on update restrict on delete cascade,
	type valuetypeenum not null,
	name varchar(200),
	values character varying[] not null
);
