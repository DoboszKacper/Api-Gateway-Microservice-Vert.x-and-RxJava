import java.sql.Date;
import java.time.LocalDate;

public class Holiday {
    private int holidayId;
    private int userId;
    private String place;
    private LocalDate date;

    public Holiday(int userId, String place, LocalDate date) {
        this.userId = userId;
        this.place = place;
        this.date = date;
    }

    public Holiday() {
    }

    public int getHolidayId() {
        return holidayId;
    }

    public void setHolidayId(int holidayId) {
        this.holidayId = holidayId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getPlace() {
        return place;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "Holiday{" +
                "holidayId=" + holidayId +
                ", userId=" + userId +
                ", place='" + place + '\'' +
                ", date=" + date +
                '}';
    }
}
