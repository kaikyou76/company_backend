--
-- PostgreSQL database dump
--

-- Dumped from database version 15.13
-- Dumped by pg_dump version 15.13

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: attendance_records; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attendance_records (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    type character varying(10) NOT NULL,
    "timestamp" timestamp with time zone NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT attendance_records_type_check CHECK (((type)::text = ANY ((ARRAY['in'::character varying, 'out'::character varying])::text[])))
);


ALTER TABLE public.attendance_records OWNER TO postgres;

--
-- Name: attendance_records_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.attendance_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.attendance_records_id_seq OWNER TO postgres;

--
-- Name: attendance_records_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.attendance_records_id_seq OWNED BY public.attendance_records.id;


--
-- Name: attendance_summaries; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.attendance_summaries (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    target_date date NOT NULL,
    total_hours numeric(5,2) DEFAULT 0.00 NOT NULL,
    overtime_hours numeric(5,2) DEFAULT 0.00 NOT NULL,
    late_night_hours numeric(5,2) DEFAULT 0.00 NOT NULL,
    holiday_hours numeric(5,2) DEFAULT 0.00 NOT NULL,
    summary_type character varying(20) DEFAULT 'daily'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT attendance_summaries_summary_type_check CHECK (((summary_type)::text = ANY ((ARRAY['daily'::character varying, 'monthly'::character varying])::text[])))
);


ALTER TABLE public.attendance_summaries OWNER TO postgres;

--
-- Name: attendance_summaries_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.attendance_summaries_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.attendance_summaries_id_seq OWNER TO postgres;

--
-- Name: attendance_summaries_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.attendance_summaries_id_seq OWNED BY public.attendance_summaries.id;


--
-- Name: batch_job_execution; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_job_execution (
    job_execution_id bigint NOT NULL,
    version bigint,
    job_instance_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);


ALTER TABLE public.batch_job_execution OWNER TO postgres;

--
-- Name: batch_job_execution_context; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_job_execution_context (
    job_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);


ALTER TABLE public.batch_job_execution_context OWNER TO postgres;

--
-- Name: batch_job_execution_params; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_job_execution_params (
    job_execution_id bigint NOT NULL,
    parameter_name character varying(100) NOT NULL,
    parameter_type character varying(100) NOT NULL,
    parameter_value character varying(2500),
    identifying character(1) NOT NULL
);


ALTER TABLE public.batch_job_execution_params OWNER TO postgres;

--
-- Name: batch_job_execution_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.batch_job_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.batch_job_execution_seq OWNER TO postgres;

--
-- Name: batch_job_instance; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_job_instance (
    job_instance_id bigint NOT NULL,
    version bigint,
    job_name character varying(100) NOT NULL,
    job_key character varying(32) NOT NULL
);


ALTER TABLE public.batch_job_instance OWNER TO postgres;

--
-- Name: batch_job_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.batch_job_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.batch_job_seq OWNER TO postgres;

--
-- Name: batch_step_execution; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_step_execution (
    step_execution_id bigint NOT NULL,
    version bigint NOT NULL,
    step_name character varying(100) NOT NULL,
    job_execution_id bigint NOT NULL,
    create_time timestamp without time zone NOT NULL,
    start_time timestamp without time zone,
    end_time timestamp without time zone,
    status character varying(10),
    commit_count bigint,
    read_count bigint,
    filter_count bigint,
    write_count bigint,
    read_skip_count bigint,
    write_skip_count bigint,
    process_skip_count bigint,
    rollback_count bigint,
    exit_code character varying(2500),
    exit_message character varying(2500),
    last_updated timestamp without time zone
);


ALTER TABLE public.batch_step_execution OWNER TO postgres;

--
-- Name: batch_step_execution_context; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.batch_step_execution_context (
    step_execution_id bigint NOT NULL,
    short_context character varying(2500) NOT NULL,
    serialized_context text
);


ALTER TABLE public.batch_step_execution_context OWNER TO postgres;

--
-- Name: batch_step_execution_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.batch_step_execution_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.batch_step_execution_seq OWNER TO postgres;

--
-- Name: departments; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.departments (
    id bigint NOT NULL,
    name text NOT NULL,
    code text NOT NULL,
    manager_id integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.departments OWNER TO postgres;

--
-- Name: departments_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.departments_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.departments_id_seq OWNER TO postgres;

--
-- Name: departments_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.departments_id_seq OWNED BY public.departments.id;


--
-- Name: holidays; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.holidays (
    id bigint NOT NULL,
    date date NOT NULL,
    name text NOT NULL,
    is_recurring boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.holidays OWNER TO postgres;

--
-- Name: holidays_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.holidays_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.holidays_id_seq OWNER TO postgres;

--
-- Name: holidays_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.holidays_id_seq OWNED BY public.holidays.id;


--
-- Name: ip_whitelist; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.ip_whitelist (
    id bigint NOT NULL,
    ip_address cidr NOT NULL,
    description text,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.ip_whitelist OWNER TO postgres;

--
-- Name: ip_whitelist_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.ip_whitelist_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.ip_whitelist_id_seq OWNER TO postgres;

--
-- Name: ip_whitelist_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.ip_whitelist_id_seq OWNED BY public.ip_whitelist.id;


--
-- Name: leave_requests; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.leave_requests (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    type character varying(20) NOT NULL,
    status character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    reason text,
    approver_id integer,
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT leave_requests_status_check CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying])::text[]))),
    CONSTRAINT leave_requests_type_check CHECK (((type)::text = ANY ((ARRAY['paid'::character varying, 'sick'::character varying, 'special'::character varying])::text[])))
);


ALTER TABLE public.leave_requests OWNER TO postgres;

--
-- Name: leave_requests_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.leave_requests_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.leave_requests_id_seq OWNER TO postgres;

--
-- Name: leave_requests_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.leave_requests_id_seq OWNED BY public.leave_requests.id;


--
-- Name: notifications; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.notifications (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    title text NOT NULL,
    message text NOT NULL,
    type character varying(20) NOT NULL,
    is_read boolean DEFAULT false NOT NULL,
    related_id integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT notifications_type_check CHECK (((type)::text = ANY ((ARRAY['leave'::character varying, 'correction'::character varying, 'system'::character varying])::text[])))
);


ALTER TABLE public.notifications OWNER TO postgres;

--
-- Name: notifications_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.notifications_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.notifications_id_seq OWNER TO postgres;

--
-- Name: notifications_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.notifications_id_seq OWNED BY public.notifications.id;


--
-- Name: overtime_reports; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.overtime_reports (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    target_month date NOT NULL,
    total_overtime numeric(5,2) DEFAULT 0.00 NOT NULL,
    total_late_night numeric(5,2) DEFAULT 0.00 NOT NULL,
    total_holiday numeric(5,2) DEFAULT 0.00 NOT NULL,
    status character varying(20) DEFAULT 'draft'::character varying NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT overtime_reports_status_check CHECK (((status)::text = ANY ((ARRAY['draft'::character varying, 'confirmed'::character varying, 'approved'::character varying])::text[])))
);


ALTER TABLE public.overtime_reports OWNER TO postgres;

--
-- Name: overtime_reports_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.overtime_reports_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.overtime_reports_id_seq OWNER TO postgres;

--
-- Name: overtime_reports_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.overtime_reports_id_seq OWNED BY public.overtime_reports.id;


--
-- Name: positions; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.positions (
    id bigint NOT NULL,
    name text NOT NULL,
    level integer NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);


ALTER TABLE public.positions OWNER TO postgres;

--
-- Name: positions_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.positions_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.positions_id_seq OWNER TO postgres;

--
-- Name: positions_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.positions_id_seq OWNED BY public.positions.id;


--
-- Name: system_logs; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.system_logs (
    id bigint NOT NULL,
    user_id integer,
    action text NOT NULL,
    status character varying(20) NOT NULL,
    ip_address text,
    user_agent text,
    details jsonb,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT system_logs_status_check CHECK (((status)::text = ANY ((ARRAY['success'::character varying, 'error'::character varying, 'warning'::character varying])::text[])))
);


ALTER TABLE public.system_logs OWNER TO postgres;

--
-- Name: system_logs_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.system_logs_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.system_logs_id_seq OWNER TO postgres;

--
-- Name: system_logs_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.system_logs_id_seq OWNED BY public.system_logs.id;


--
-- Name: time_corrections; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.time_corrections (
    id bigint NOT NULL,
    user_id integer NOT NULL,
    attendance_id bigint NOT NULL,
    request_type character varying(20) NOT NULL,
    before_time timestamp with time zone NOT NULL,
    current_type character varying(10) NOT NULL,
    requested_time timestamp with time zone,
    requested_type character varying(10),
    reason text NOT NULL,
    status character varying(20) DEFAULT 'pending'::character varying NOT NULL,
    approver_id integer,
    approved_at timestamp with time zone,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT time_corrections_current_type_check CHECK (((current_type)::text = ANY ((ARRAY['in'::character varying, 'out'::character varying])::text[]))),
    CONSTRAINT time_corrections_request_type_check CHECK (((request_type)::text = ANY ((ARRAY['time'::character varying, 'type'::character varying, 'both'::character varying])::text[]))),
    CONSTRAINT time_corrections_requested_type_check CHECK (((requested_type)::text = ANY ((ARRAY['in'::character varying, 'out'::character varying])::text[]))),
    CONSTRAINT time_corrections_status_check CHECK (((status)::text = ANY ((ARRAY['pending'::character varying, 'approved'::character varying, 'rejected'::character varying])::text[])))
);


ALTER TABLE public.time_corrections OWNER TO postgres;

--
-- Name: time_corrections_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.time_corrections_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.time_corrections_id_seq OWNER TO postgres;

--
-- Name: time_corrections_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.time_corrections_id_seq OWNED BY public.time_corrections.id;


--
-- Name: users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.users (
    id bigint NOT NULL,
    username text NOT NULL,
    password_hash text NOT NULL,
    location_type character varying(20) NOT NULL,
    client_latitude double precision,
    client_longitude double precision,
    manager_id integer,
    department_id integer,
    position_id integer,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT users_location_type_check CHECK (((location_type)::text = ANY ((ARRAY['office'::character varying, 'client'::character varying])::text[])))
);


ALTER TABLE public.users OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.users_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.users_id_seq OWNER TO postgres;

--
-- Name: users_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.users_id_seq OWNED BY public.users.id;


--
-- Name: work_locations; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.work_locations (
    id bigint NOT NULL,
    name text NOT NULL,
    type character varying(20) NOT NULL,
    latitude double precision NOT NULL,
    longitude double precision NOT NULL,
    radius integer DEFAULT 100 NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT work_locations_type_check CHECK (((type)::text = ANY ((ARRAY['office'::character varying, 'client'::character varying, 'other'::character varying])::text[])))
);


ALTER TABLE public.work_locations OWNER TO postgres;

--
-- Name: work_locations_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE public.work_locations_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER TABLE public.work_locations_id_seq OWNER TO postgres;

--
-- Name: work_locations_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

ALTER SEQUENCE public.work_locations_id_seq OWNED BY public.work_locations.id;


--
-- Name: attendance_records id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_records ALTER COLUMN id SET DEFAULT nextval('public.attendance_records_id_seq'::regclass);


--
-- Name: attendance_summaries id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_summaries ALTER COLUMN id SET DEFAULT nextval('public.attendance_summaries_id_seq'::regclass);


--
-- Name: departments id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments ALTER COLUMN id SET DEFAULT nextval('public.departments_id_seq'::regclass);


--
-- Name: holidays id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.holidays ALTER COLUMN id SET DEFAULT nextval('public.holidays_id_seq'::regclass);


--
-- Name: ip_whitelist id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ip_whitelist ALTER COLUMN id SET DEFAULT nextval('public.ip_whitelist_id_seq'::regclass);


--
-- Name: leave_requests id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.leave_requests ALTER COLUMN id SET DEFAULT nextval('public.leave_requests_id_seq'::regclass);


--
-- Name: notifications id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications ALTER COLUMN id SET DEFAULT nextval('public.notifications_id_seq'::regclass);


--
-- Name: overtime_reports id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.overtime_reports ALTER COLUMN id SET DEFAULT nextval('public.overtime_reports_id_seq'::regclass);


--
-- Name: positions id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.positions ALTER COLUMN id SET DEFAULT nextval('public.positions_id_seq'::regclass);


--
-- Name: system_logs id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_logs ALTER COLUMN id SET DEFAULT nextval('public.system_logs_id_seq'::regclass);


--
-- Name: time_corrections id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_corrections ALTER COLUMN id SET DEFAULT nextval('public.time_corrections_id_seq'::regclass);


--
-- Name: users id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users ALTER COLUMN id SET DEFAULT nextval('public.users_id_seq'::regclass);


--
-- Name: work_locations id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_locations ALTER COLUMN id SET DEFAULT nextval('public.work_locations_id_seq'::regclass);


--
-- Data for Name: attendance_records; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.attendance_records (id, user_id, type, "timestamp", latitude, longitude, created_at) FROM stdin;
1	1	in	2024-12-01 08:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
2	1	out	2024-12-01 20:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
3	1	in	2024-12-02 08:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
4	1	out	2024-12-02 19:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
5	2	in	2024-12-01 08:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
6	2	out	2024-12-01 19:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
7	2	in	2024-12-02 08:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
8	2	out	2024-12-02 19:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
9	3	in	2024-12-01 08:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
10	3	out	2024-12-01 18:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
11	3	in	2024-12-02 08:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
12	3	out	2024-12-02 18:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.255599+09
13	4	in	2024-12-01 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
14	4	out	2024-12-01 18:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
15	4	in	2024-12-02 09:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
16	4	out	2024-12-02 18:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
17	5	in	2024-12-01 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
18	5	out	2024-12-01 19:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
19	5	in	2024-12-02 09:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
20	5	out	2024-12-02 19:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
21	6	in	2024-12-01 08:30:00+09	35.6762	139.6503	2025-07-24 07:33:02.26087+09
22	6	out	2024-12-01 17:30:00+09	35.6762	139.6503	2025-07-24 07:33:02.26087+09
23	6	in	2024-12-02 08:45:00+09	35.6762	139.6503	2025-07-24 07:33:02.26087+09
24	6	out	2024-12-02 17:45:00+09	35.6762	139.6503	2025-07-24 07:33:02.26087+09
25	7	in	2024-12-01 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
26	7	out	2024-12-01 18:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
27	8	in	2024-12-01 08:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
28	8	out	2024-12-01 20:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.26087+09
29	9	in	2024-12-01 09:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.261956+09
30	9	out	2024-12-01 18:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.261956+09
31	9	in	2024-12-02 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.261956+09
32	9	out	2024-12-02 19:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.261956+09
33	10	in	2024-12-01 08:45:00+09	35.6762	139.6503	2025-07-24 07:33:02.261956+09
34	10	out	2024-12-01 17:15:00+09	35.6762	139.6503	2025-07-24 07:33:02.261956+09
35	11	in	2024-12-01 09:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.261956+09
36	11	out	2024-12-01 18:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.261956+09
37	12	in	2024-12-01 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.263148+09
38	12	out	2024-12-01 18:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.263148+09
39	12	in	2024-12-02 09:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.263148+09
40	12	out	2024-12-02 18:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.263148+09
41	13	in	2024-12-01 09:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.263148+09
42	13	out	2024-12-01 18:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.263148+09
43	14	in	2024-12-01 09:00:00+09	35.6762	139.6503	2025-07-24 07:33:02.263148+09
44	14	out	2024-12-01 17:30:00+09	35.6762	139.6503	2025-07-24 07:33:02.263148+09
45	15	in	2024-12-01 09:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
46	15	out	2024-12-01 18:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
47	15	in	2024-12-02 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
48	15	out	2024-12-02 17:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
49	16	in	2024-12-01 08:55:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
50	16	out	2024-12-01 17:55:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
51	17	in	2024-12-01 08:30:00+09	35.6762	139.6503	2025-07-24 07:33:02.264102+09
52	17	out	2024-12-01 17:30:00+09	35.6762	139.6503	2025-07-24 07:33:02.264102+09
53	18	in	2024-12-01 09:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
54	18	out	2024-12-01 18:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
55	19	in	2024-12-01 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
56	19	out	2024-12-01 18:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.264102+09
57	20	in	2024-12-01 08:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.264864+09
58	20	out	2024-12-01 17:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.264864+09
59	20	in	2024-12-02 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.264864+09
60	20	out	2024-12-02 18:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.264864+09
61	21	in	2024-12-01 09:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.264864+09
62	21	out	2024-12-01 17:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.264864+09
63	22	in	2024-12-01 09:15:00+09	35.6812	139.7671	2025-07-24 07:33:02.264864+09
64	22	out	2024-12-01 17:45:00+09	35.6812	139.7671	2025-07-24 07:33:02.264864+09
65	23	in	2024-12-01 09:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.265455+09
66	23	out	2024-12-01 17:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.265455+09
67	24	in	2024-12-01 10:00:00+09	35.6812	139.7671	2025-07-24 07:33:02.265455+09
68	24	out	2024-12-01 16:30:00+09	35.6812	139.7671	2025-07-24 07:33:02.265455+09
\.


--
-- Data for Name: attendance_summaries; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.attendance_summaries (id, user_id, target_date, total_hours, overtime_hours, late_night_hours, holiday_hours, summary_type, created_at) FROM stdin;
\.


--
-- Data for Name: batch_job_execution; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_job_execution (job_execution_id, version, job_instance_id, create_time, start_time, end_time, status, exit_code, exit_message, last_updated) FROM stdin;
\.


--
-- Data for Name: batch_job_execution_context; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_job_execution_context (job_execution_id, short_context, serialized_context) FROM stdin;
\.


--
-- Data for Name: batch_job_execution_params; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_job_execution_params (job_execution_id, parameter_name, parameter_type, parameter_value, identifying) FROM stdin;
\.


--
-- Data for Name: batch_job_instance; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_job_instance (job_instance_id, version, job_name, job_key) FROM stdin;
\.


--
-- Data for Name: batch_step_execution; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_step_execution (step_execution_id, version, step_name, job_execution_id, create_time, start_time, end_time, status, commit_count, read_count, filter_count, write_count, read_skip_count, write_skip_count, process_skip_count, rollback_count, exit_code, exit_message, last_updated) FROM stdin;
\.


--
-- Data for Name: batch_step_execution_context; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.batch_step_execution_context (step_execution_id, short_context, serialized_context) FROM stdin;
\.


--
-- Data for Name: departments; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.departments (id, name, code, manager_id, created_at) FROM stdin;
4	総務部	GA	\N	2025-07-24 07:11:01.389741+09
6	法務部	LEGAL	\N	2025-07-24 07:11:01.389741+09
7	マーケティング部	MKT	\N	2025-07-24 07:11:01.389741+09
8	品質保証部	QA	\N	2025-07-24 07:11:01.389741+09
10	研究開発部	RND	\N	2025-07-24 07:11:01.389741+09
1	人事部	HR	4	2025-07-24 07:11:01.389741+09
2	開発部	DEV	5	2025-07-24 07:11:01.389741+09
3	営業部	SALES	6	2025-07-24 07:11:01.389741+09
5	経理部	FIN	7	2025-07-24 07:11:01.389741+09
9	情報システム部	IT	8	2025-07-24 07:11:01.389741+09
\.


--
-- Data for Name: holidays; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.holidays (id, date, name, is_recurring, created_at) FROM stdin;
1	2024-01-01	元日	t	2025-07-24 07:11:01.394681+09
2	2024-12-31	年末休暇	f	2025-07-24 07:11:01.394681+09
3	2025-01-01	元日	t	2025-07-24 07:11:01.394681+09
4	2025-01-13	成人の日	f	2025-07-24 07:11:01.394681+09
5	2025-02-11	建国記念の日	t	2025-07-24 07:11:01.394681+09
6	2025-04-29	昭和の日	t	2025-07-24 07:11:01.394681+09
7	2025-05-03	憲法記念日	t	2025-07-24 07:11:01.394681+09
8	2025-05-04	みどりの日	t	2025-07-24 07:11:01.394681+09
9	2025-05-05	こどもの日	t	2025-07-24 07:11:01.394681+09
\.


--
-- Data for Name: ip_whitelist; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.ip_whitelist (id, ip_address, description, is_active, created_at) FROM stdin;
1	192.168.1.0/24	本社ネットワーク	t	2025-07-24 07:11:01.396127+09
2	192.168.2.0/24	新宿支社ネットワーク	t	2025-07-24 07:11:01.396127+09
3	192.168.3.0/24	大阪支社ネットワーク	t	2025-07-24 07:11:01.396127+09
4	192.168.4.0/24	福岡支社ネットワーク	t	2025-07-24 07:11:01.396127+09
5	10.0.0.0/8	VPNアクセス	t	2025-07-24 07:11:01.396127+09
6	172.16.0.0/12	プライベートVPN	t	2025-07-24 07:11:01.396127+09
7	127.0.0.1/32	ローカルホスト	t	2025-07-24 07:11:01.396127+09
8	::1/128	IPv6ローカルホスト	t	2025-07-24 07:11:01.396127+09
9	203.0.113.0/24	クライアントAネットワーク	t	2025-07-24 07:11:01.396127+09
10	198.51.100.0/24	クライアントBネットワーク	t	2025-07-24 07:11:01.396127+09
11	203.0.113.100/32	管理者専用IP	t	2025-07-24 07:11:01.396127+09
12	192.168.99.0/24	旧ネットワーク	f	2025-07-24 07:11:01.396127+09
\.


--
-- Data for Name: leave_requests; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.leave_requests (id, user_id, type, status, start_date, end_date, reason, approver_id, approved_at, created_at, updated_at) FROM stdin;
1	15	paid	approved	2024-12-10	2024-12-10	私用のため	5	2024-12-05 10:00:00+09	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
2	17	paid	approved	2024-12-15	2024-12-16	家族旅行のため	6	2024-12-08 14:30:00+09	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
3	20	sick	approved	2024-12-08	2024-12-08	体調不良のため	5	2024-12-07 16:00:00+09	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
4	23	paid	approved	2024-12-20	2024-12-20	年末の用事のため	12	2024-12-15 11:00:00+09	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
5	16	paid	pending	2024-12-25	2024-12-25	年末休暇のため	\N	\N	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
6	18	special	pending	2024-12-30	2024-12-31	年末年始休暇	\N	\N	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
7	21	sick	pending	2024-12-12	2024-12-12	通院のため	\N	\N	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
8	24	paid	pending	2024-12-28	2024-12-28	帰省のため	\N	\N	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
9	19	paid	rejected	2024-12-24	2024-12-26	長期休暇申請	7	2024-12-18 09:30:00+09	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
10	22	special	rejected	2024-12-23	2024-12-27	年末長期休暇	5	2024-12-16 15:45:00+09	2025-07-24 07:43:04.948846+09	2025-07-24 07:43:04.948846+09
\.


--
-- Data for Name: notifications; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.notifications (id, user_id, title, message, type, is_read, related_id, created_at) FROM stdin;
1	15	休暇申請が承認されました	12月10日の有給休暇申請が開発部長により承認されました。	leave	t	1	2025-07-24 07:43:04.962404+09
2	17	休暇申請が承認されました	12月15-16日の有給休暇申請が営業部長により承認されました。	leave	t	2	2025-07-24 07:43:04.962404+09
3	20	病気休暇申請が承認されました	12月8日の病気休暇申請が承認されました。お大事にしてください。	leave	f	3	2025-07-24 07:43:04.962404+09
4	5	新しい休暇申請があります	高橋さんから年末休暇の申請が提出されました。承認をお願いします。	leave	f	5	2025-07-24 07:43:04.962404+09
5	7	新しい休暇申請があります	小林さんから特別休暇の申請が提出されました。	leave	f	6	2025-07-24 07:43:04.962404+09
6	5	新しい病気休暇申請	新人の佐藤さんから病気休暇の申請があります。	leave	f	7	2025-07-24 07:43:04.962404+09
7	19	休暇申請が却下されました	12月24-26日の休暇申請は業務都合により却下されました。	leave	f	9	2025-07-24 07:43:04.962404+09
8	22	特別休暇申請が却下されました	年末長期休暇申請は承認できませんでした。	leave	f	10	2025-07-24 07:43:04.962404+09
9	1	セキュリティアラート	不正なIPアドレスからのアクセス試行が検出されました。	system	f	\N	2025-07-24 07:43:04.962404+09
10	3	システムメンテナンス予定	12月31日 23:00-1:00にシステムメンテナンスを実施します。	system	f	\N	2025-07-24 07:43:04.962404+09
11	5	勤怠データ異常検知	位置情報が範囲外の打刻が検出されました。確認をお願いします。	system	f	\N	2025-07-24 07:43:04.962404+09
12	12	打刻修正申請が必要です	12月1日の退勤打刻が記録されていません。修正申請を提出してください。	correction	f	\N	2025-07-24 07:43:04.962404+09
13	5	打刻修正申請があります	田中主任から打刻修正申請が提出されました。	correction	f	\N	2025-07-24 07:43:04.962404+09
\.


--
-- Data for Name: overtime_reports; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.overtime_reports (id, user_id, target_month, total_overtime, total_late_night, total_holiday, status, created_at, updated_at) FROM stdin;
\.


--
-- Data for Name: positions; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.positions (id, name, level, created_at) FROM stdin;
1	CEO	10	2025-07-24 07:11:01.385284+09
2	取締役	9	2025-07-24 07:11:01.385284+09
3	部長	8	2025-07-24 07:11:01.385284+09
4	副部長	7	2025-07-24 07:11:01.385284+09
5	課長	6	2025-07-24 07:11:01.385284+09
6	副課長	5	2025-07-24 07:11:01.385284+09
7	主任	4	2025-07-24 07:11:01.385284+09
8	リーダー	3	2025-07-24 07:11:01.385284+09
9	一般社員	2	2025-07-24 07:11:01.385284+09
10	新入社員	1	2025-07-24 07:11:01.385284+09
11	インターン	0	2025-07-24 07:11:01.385284+09
\.


--
-- Data for Name: system_logs; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.system_logs (id, user_id, action, status, ip_address, user_agent, details, created_at) FROM stdin;
1	1	LOGIN	success	192.168.1.100	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"role": "CEO", "method": "password"}	2025-07-24 07:43:04.959047+09
2	3	LOGIN	success	192.168.1.101	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"role": "ADMIN", "method": "password"}	2025-07-24 07:43:04.959047+09
3	5	LOGIN	success	192.168.1.102	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"role": "MANAGER", "method": "password"}	2025-07-24 07:43:04.959047+09
4	20	LOGIN	success	192.168.1.103	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"role": "EMPLOYEE", "method": "password"}	2025-07-24 07:43:04.959047+09
5	12	ATTENDANCE_CLOCK_IN	success	192.168.1.104	CompanyApp/1.0	{"latitude": 35.6812, "location": "本社オフィス", "longitude": 139.7671}	2025-07-24 07:43:04.959047+09
6	15	ATTENDANCE_CLOCK_OUT	success	192.168.1.105	CompanyApp/1.0	{"latitude": 35.6812, "location": "本社オフィス", "longitude": 139.7671}	2025-07-24 07:43:04.959047+09
7	17	ATTENDANCE_CLOCK_IN	success	203.0.113.50	CompanyApp/1.0	{"latitude": 35.6762, "location": "クライアントA", "longitude": 139.6503}	2025-07-24 07:43:04.959047+09
8	23	ACCESS_ADMIN_PANEL	error	192.168.1.106	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"error": "insufficient_privileges", "required_role": "ADMIN"}	2025-07-24 07:43:04.959047+09
9	24	VIEW_ALL_USERS	error	192.168.1.107	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"error": "access_denied", "required_level": 5}	2025-07-24 07:43:04.959047+09
10	16	ATTENDANCE_CLOCK_IN	error	192.168.1.108	CompanyApp/1.0	{"error": "location_out_of_range", "latitude": 35.7000, "longitude": 139.8000}	2025-07-24 07:43:04.959047+09
11	\N	LOGIN_ATTEMPT	warning	203.0.113.999	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"error": "ip_not_whitelisted", "blocked": true}	2025-07-24 07:43:04.959047+09
12	5	LEAVE_REQUEST_APPROVE	success	192.168.1.109	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"action": "approve", "user_id": 15, "request_id": 1}	2025-07-24 07:43:04.959047+09
13	7	LEAVE_REQUEST_REJECT	success	192.168.1.110	Mozilla/5.0 (Windows NT 10.0; Win64; x64)	{"action": "reject", "user_id": 19, "request_id": 9}	2025-07-24 07:43:04.959047+09
\.


--
-- Data for Name: time_corrections; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.time_corrections (id, user_id, attendance_id, request_type, before_time, current_type, requested_time, requested_type, reason, status, approver_id, approved_at, created_at) FROM stdin;
\.


--
-- Data for Name: users; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.users (id, username, password_hash, location_type, client_latitude, client_longitude, manager_id, department_id, position_id, created_at, updated_at) FROM stdin;
1	ceo@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	\N	1	1	2025-07-24 07:19:49.275446+09	2025-07-24 07:19:49.275446+09
11	qa_lead@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	\N	8	5	2025-07-24 07:19:49.283+09	2025-07-24 07:19:49.283+09
19	kato@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	\N	8	9	2025-07-24 07:19:49.284101+09	2025-07-24 07:19:49.284101+09
2	director@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	1	1	2	2025-07-24 07:19:49.275446+09	2025-07-24 07:19:49.275446+09
3	admin@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	1	1	3	2025-07-24 07:19:49.275446+09	2025-07-24 07:19:49.275446+09
4	hr_manager@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	3	1	3	2025-07-24 07:19:49.282199+09	2025-07-24 07:19:49.282199+09
5	dev_manager@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	3	2	3	2025-07-24 07:19:49.282199+09	2025-07-24 07:19:49.282199+09
6	sales_manager@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	client	\N	\N	3	3	3	2025-07-24 07:19:49.282199+09	2025-07-24 07:19:49.282199+09
7	finance_manager@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	3	5	3	2025-07-24 07:19:49.282199+09	2025-07-24 07:19:49.282199+09
8	it_manager@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	3	9	3	2025-07-24 07:19:49.282199+09	2025-07-24 07:19:49.282199+09
9	dev_lead@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	5	2	5	2025-07-24 07:19:49.283+09	2025-07-24 07:19:49.283+09
12	tanaka@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	5	2	7	2025-07-24 07:19:49.28357+09	2025-07-24 07:19:49.28357+09
13	suzuki@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	5	2	8	2025-07-24 07:19:49.28357+09	2025-07-24 07:19:49.28357+09
10	sales_lead@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	client	\N	\N	6	3	5	2025-07-24 07:19:49.283+09	2025-07-24 07:19:49.283+09
14	yamada@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	client	\N	\N	6	3	8	2025-07-24 07:19:49.28357+09	2025-07-24 07:19:49.28357+09
15	watanabe@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	9	2	9	2025-07-24 07:19:49.284101+09	2025-07-24 07:19:49.284101+09
16	takahashi@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	9	2	9	2025-07-24 07:19:49.284101+09	2025-07-24 07:19:49.284101+09
20	employee@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	9	2	10	2025-07-24 07:19:49.284678+09	2025-07-24 07:19:49.284678+09
22	junior@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	9	2	10	2025-07-24 07:19:49.284678+09	2025-07-24 07:19:49.284678+09
23	intern1@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	9	2	11	2025-07-24 07:19:49.285182+09	2025-07-24 07:19:49.285182+09
17	ito@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	client	\N	\N	10	3	9	2025-07-24 07:19:49.284101+09	2025-07-24 07:19:49.284101+09
18	kobayashi@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	7	5	9	2025-07-24 07:19:49.284101+09	2025-07-24 07:19:49.284101+09
21	newbie@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	12	3	10	2025-07-24 07:19:49.284678+09	2025-07-24 07:19:49.284678+09
24	intern2@company.com	$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjPHSxw5Z6	office	\N	\N	13	7	11	2025-07-24 07:19:49.285182+09	2025-07-24 07:19:49.285182+09
\.


--
-- Data for Name: work_locations; Type: TABLE DATA; Schema: public; Owner: postgres
--

COPY public.work_locations (id, name, type, latitude, longitude, radius, is_active, created_at) FROM stdin;
1	本社オフィス	office	35.6812	139.7671	100	t	2025-07-24 07:11:01.391836+09
2	新宿支社	office	35.6896	139.7006	80	t	2025-07-24 07:11:01.391836+09
3	大阪支社	office	34.7024	135.4959	120	t	2025-07-24 07:11:01.391836+09
4	福岡支社	office	33.5904	130.4017	100	t	2025-07-24 07:11:01.391836+09
5	クライアントA本社	client	35.6762	139.6503	50	t	2025-07-24 07:11:01.391836+09
6	クライアントA支社	client	35.6895	139.6917	50	t	2025-07-24 07:11:01.391836+09
7	クライアントB	client	35.658	139.7414	60	t	2025-07-24 07:11:01.391836+09
8	クライアントC	client	35.6284	139.7387	40	t	2025-07-24 07:11:01.391836+09
9	研修センター	other	35.6938	139.7034	200	t	2025-07-24 07:11:01.391836+09
10	データセンター	other	35.6654	139.7707	30	t	2025-07-24 07:11:01.391836+09
11	旧オフィス	office	35.65	139.75	50	f	2025-07-24 07:11:01.391836+09
\.


--
-- Name: attendance_records_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.attendance_records_id_seq', 68, true);


--
-- Name: attendance_summaries_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.attendance_summaries_id_seq', 1, false);


--
-- Name: batch_job_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.batch_job_execution_seq', 1, false);


--
-- Name: batch_job_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.batch_job_seq', 1, false);


--
-- Name: batch_step_execution_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.batch_step_execution_seq', 1, false);


--
-- Name: departments_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.departments_id_seq', 10, true);


--
-- Name: holidays_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.holidays_id_seq', 9, true);


--
-- Name: ip_whitelist_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.ip_whitelist_id_seq', 12, true);


--
-- Name: leave_requests_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.leave_requests_id_seq', 10, true);


--
-- Name: notifications_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.notifications_id_seq', 13, true);


--
-- Name: overtime_reports_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.overtime_reports_id_seq', 1, false);


--
-- Name: positions_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.positions_id_seq', 11, true);


--
-- Name: system_logs_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.system_logs_id_seq', 13, true);


--
-- Name: time_corrections_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.time_corrections_id_seq', 1, false);


--
-- Name: users_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.users_id_seq', 24, true);


--
-- Name: work_locations_id_seq; Type: SEQUENCE SET; Schema: public; Owner: postgres
--

SELECT pg_catalog.setval('public.work_locations_id_seq', 11, true);


--
-- Name: attendance_records attendance_records_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_records
    ADD CONSTRAINT attendance_records_pkey PRIMARY KEY (id);


--
-- Name: attendance_summaries attendance_summaries_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_summaries
    ADD CONSTRAINT attendance_summaries_pkey PRIMARY KEY (id);


--
-- Name: batch_job_execution_context batch_job_execution_context_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution_context
    ADD CONSTRAINT batch_job_execution_context_pkey PRIMARY KEY (job_execution_id);


--
-- Name: batch_job_execution batch_job_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution
    ADD CONSTRAINT batch_job_execution_pkey PRIMARY KEY (job_execution_id);


--
-- Name: batch_job_instance batch_job_instance_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_instance
    ADD CONSTRAINT batch_job_instance_pkey PRIMARY KEY (job_instance_id);


--
-- Name: batch_step_execution_context batch_step_execution_context_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_step_execution_context
    ADD CONSTRAINT batch_step_execution_context_pkey PRIMARY KEY (step_execution_id);


--
-- Name: batch_step_execution batch_step_execution_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_step_execution
    ADD CONSTRAINT batch_step_execution_pkey PRIMARY KEY (step_execution_id);


--
-- Name: departments departments_code_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_code_key UNIQUE (code);


--
-- Name: departments departments_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT departments_pkey PRIMARY KEY (id);


--
-- Name: holidays holidays_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.holidays
    ADD CONSTRAINT holidays_pkey PRIMARY KEY (id);


--
-- Name: ip_whitelist ip_whitelist_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.ip_whitelist
    ADD CONSTRAINT ip_whitelist_pkey PRIMARY KEY (id);


--
-- Name: batch_job_instance job_inst_un; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_instance
    ADD CONSTRAINT job_inst_un UNIQUE (job_name, job_key);


--
-- Name: leave_requests leave_requests_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.leave_requests
    ADD CONSTRAINT leave_requests_pkey PRIMARY KEY (id);


--
-- Name: notifications notifications_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_pkey PRIMARY KEY (id);


--
-- Name: overtime_reports overtime_reports_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.overtime_reports
    ADD CONSTRAINT overtime_reports_pkey PRIMARY KEY (id);


--
-- Name: positions positions_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.positions
    ADD CONSTRAINT positions_pkey PRIMARY KEY (id);


--
-- Name: system_logs system_logs_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_logs
    ADD CONSTRAINT system_logs_pkey PRIMARY KEY (id);


--
-- Name: time_corrections time_corrections_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_corrections
    ADD CONSTRAINT time_corrections_pkey PRIMARY KEY (id);


--
-- Name: users users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_pkey PRIMARY KEY (id);


--
-- Name: users users_username_key; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_username_key UNIQUE (username);


--
-- Name: work_locations work_locations_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.work_locations
    ADD CONSTRAINT work_locations_pkey PRIMARY KEY (id);


--
-- Name: idx_attendance_summaries_user_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_summaries_user_date ON public.attendance_summaries USING btree (user_id, target_date);


--
-- Name: idx_attendance_user_timestamp; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_attendance_user_timestamp ON public.attendance_records USING btree (user_id, "timestamp");


--
-- Name: idx_holidays_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_holidays_date ON public.holidays USING btree (date);


--
-- Name: idx_ip_whitelist_address; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_ip_whitelist_address ON public.ip_whitelist USING btree (ip_address);


--
-- Name: idx_leave_requests_user_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_leave_requests_user_status ON public.leave_requests USING btree (user_id, status);


--
-- Name: idx_notifications_user_read; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_notifications_user_read ON public.notifications USING btree (user_id, is_read);


--
-- Name: idx_overtime_reports_user_month; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_overtime_reports_user_month ON public.overtime_reports USING btree (user_id, target_month);


--
-- Name: idx_system_logs_action_date; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_system_logs_action_date ON public.system_logs USING btree (action, created_at);


--
-- Name: idx_time_corrections_user_status; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_time_corrections_user_status ON public.time_corrections USING btree (user_id, status);


--
-- Name: idx_users_username; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX idx_users_username ON public.users USING btree (username);


--
-- Name: idx_work_locations_name; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_work_locations_name ON public.work_locations USING btree (name);


--
-- Name: attendance_records attendance_records_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_records
    ADD CONSTRAINT attendance_records_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: attendance_summaries attendance_summaries_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.attendance_summaries
    ADD CONSTRAINT attendance_summaries_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: departments fk_departments_manager; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.departments
    ADD CONSTRAINT fk_departments_manager FOREIGN KEY (manager_id) REFERENCES public.users(id);


--
-- Name: batch_job_execution_context job_exec_ctx_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution_context
    ADD CONSTRAINT job_exec_ctx_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);


--
-- Name: batch_job_execution_params job_exec_params_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution_params
    ADD CONSTRAINT job_exec_params_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);


--
-- Name: batch_step_execution job_exec_step_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_step_execution
    ADD CONSTRAINT job_exec_step_fk FOREIGN KEY (job_execution_id) REFERENCES public.batch_job_execution(job_execution_id);


--
-- Name: batch_job_execution job_inst_exec_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_job_execution
    ADD CONSTRAINT job_inst_exec_fk FOREIGN KEY (job_instance_id) REFERENCES public.batch_job_instance(job_instance_id);


--
-- Name: leave_requests leave_requests_approver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.leave_requests
    ADD CONSTRAINT leave_requests_approver_id_fkey FOREIGN KEY (approver_id) REFERENCES public.users(id);


--
-- Name: leave_requests leave_requests_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.leave_requests
    ADD CONSTRAINT leave_requests_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: notifications notifications_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.notifications
    ADD CONSTRAINT notifications_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: overtime_reports overtime_reports_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.overtime_reports
    ADD CONSTRAINT overtime_reports_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: batch_step_execution_context step_exec_ctx_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.batch_step_execution_context
    ADD CONSTRAINT step_exec_ctx_fk FOREIGN KEY (step_execution_id) REFERENCES public.batch_step_execution(step_execution_id);


--
-- Name: system_logs system_logs_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.system_logs
    ADD CONSTRAINT system_logs_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: time_corrections time_corrections_approver_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_corrections
    ADD CONSTRAINT time_corrections_approver_id_fkey FOREIGN KEY (approver_id) REFERENCES public.users(id);


--
-- Name: time_corrections time_corrections_attendance_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_corrections
    ADD CONSTRAINT time_corrections_attendance_id_fkey FOREIGN KEY (attendance_id) REFERENCES public.attendance_records(id);


--
-- Name: time_corrections time_corrections_user_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.time_corrections
    ADD CONSTRAINT time_corrections_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.users(id);


--
-- Name: users users_department_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_department_id_fkey FOREIGN KEY (department_id) REFERENCES public.departments(id);


--
-- Name: users users_manager_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_manager_id_fkey FOREIGN KEY (manager_id) REFERENCES public.users(id);


--
-- Name: users users_position_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.users
    ADD CONSTRAINT users_position_id_fkey FOREIGN KEY (position_id) REFERENCES public.positions(id);


--
-- PostgreSQL database dump complete
--

