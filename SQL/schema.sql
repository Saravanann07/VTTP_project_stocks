drop schema if exists stocks;

create schema stocks;

use stocks;

create table users (
	user_id INT NOT NULL AUTO_INCREMENT,
    username varchar(32) not null,
    password varchar(256) not null,

    primary key(user_id)
);

create table transactions (
	transaction_id int not null auto_increment,
    purchase_date date,
    symbol char (8) not null,
    company_name varchar(64),
    quantity int not null,
    stock_price float (7,2) not null,
    total_price float (10,2) not null,
    stock_status float (10,2) not null,
    user_id int not null,
    
    foreign key (user_id) references users (user_id),
    
    primary key(transaction_id)
    );