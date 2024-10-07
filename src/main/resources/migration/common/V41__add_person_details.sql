create table if not exists person_location
(
    prison_number varchar(10) primary key not null,
    first_name    varchar(64)             not null,
    last_name     varchar(64)             not null,
    status        varchar(16)             not null,
    prison_code   varchar(16),
    cell_location varchar(64),
    version       int                     not null
);

create table if not exists person_location_audit
(
    rev_id                 bigint      not null references audit_revision (id),
    rev_type               smallint    not null,
    prison_number          varchar(10) not null,
    first_name             varchar(64) not null,
    last_name              varchar(64) not null,
    status                 varchar(16) not null,
    prison_code            varchar(16),
    cell_location          varchar(64),

    first_name_modified    boolean     not null,
    last_name_modified     boolean     not null,
    status_modified        boolean     not null,
    prison_code_modified   boolean     not null,
    cell_location_modified boolean     not null,
    primary key (rev_id, prison_number)
);

with csip_person as (select prison_number, '', '', 'ACTIVE IN', null, null from csip_record group by prison_number)
insert
into person_location
select *
from csip_person;

alter table csip_record
    add constraint fk_csip_prison_number foreign key (prison_number) references person_location (prison_number);