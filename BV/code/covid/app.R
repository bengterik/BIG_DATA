#
# This is a Shiny web application. You can run the application by clicking
# the 'Run App' button above.
#
# Find out more about building applications with Shiny here:
#
#    http://shiny.rstudio.com/
#


#install.packages("dplyr")

#source("covid/helpers.R")
# tryCatch(
#   {library(shiny)},
#   finally = install.packages("shiny")
# )
# tryCatch(
#   {library(maps)},
#   finally = install.packages("tmap")
# )
# tryCatch(
#   {library(mapproj)},
#   finally = install.packages("mapproj")
# )
# tryCatch(
#   {library(rworldmap)},
#   finally = install.packages("rworldmap")
# )
source("helpers.R")
library(highcharter)
library(ggplot2)
library(magrittr)
library(leaflet)
library(shiny)
library(jsonlite)
library(maps)
library(mapproj)
library(leaflet.providers)
library(dplyr)



data <- read.csv("deaths_covid.csv")
View(head(data,10))
all_columns <- colnames(data, do.NULL = TRUE, prefix = "col")
columns_wanted <- c("iso_code", "continent", "location", "date",
                   "total_cases_per_million", "total_deaths_per_million", 
                   "reproduction_rate", "total_boosters_per_hundred", 
                   "aged_65_older")

data_wanted <- data[columns_wanted]
#data_wanted[is.na(data_wanted)] <- 0
data_wanted <- mutate_all(data_wanted, ~replace(., is.na(.), 0))
View(data_wanted)

# get the dates as a column to return the first and last day
dates <- as.Date(data_wanted$date)
first_day <- min(dates)
last_day <- max(dates)
chosen_day <- first_day

#continents <- dplyr::distinct(data_wanted, continent)[!apply(is.na(dplyr::distinct(data_wanted, continent)) | dplyr::distinct(data_wanted, continent) == "", 1, all),]



# Define UI for application that draws a histogram
ui <- shinyUI(
  fluidPage(
   
   # Application title
   titlePanel("Covid Data"),
   
   # Sidebar with a slider input for the choosen date
   
    fluidRow(
      sidebarPanel(
        fluidRow(
          column(10,sliderInput("date",
                     "Choose the date:",
                     min = first_day,
                     max = last_day,
                     value = chosen_day))),
         fluidRow(
           column(10,
                  selectInput("chosen_variable", h3("Desired variable:"),
                              choices = list("total cases per million" = "total_cases_per_million" ,
                                             "total deaths per million" = "total_deaths_per_million" ,
                                             "reproduction rate" = "reproduction_rate" ,
                                              "total boosters per hundred" = "total_boosters_per_hundred" , 
                                              "aged 65 older" = "aged_65_older")), selected = "total_deaths_per_million")),
        fluidRow(
          column(10,
                 selectInput("chosen_continent", h3("Choose the continent:"),
                             choices = list("Africa" = "custom/africa",
                                            "Asia" = "custom/asia",
                                            "Europe" = "custom/europe",
                                            "North America" = "custom/north-america",
                                            "South America" = "custom/south-america",
                                            "Oceania" = "custom/oceania",
                                            "The whole world" = "custom/world-robinson-lowres")), 
                 selected = "The whole world"))),
        mainPanel(
          highchartOutput("myMap") 
          )
      ),
   fluidRow(
     sidebarLayout(
       
       sidebarPanel(
         sliderInput(
           "bins", label = "Number of bins:",
           min = 1, value = 30, max = 50
         )
       ),
       
       mainPanel(
         plotOutput("distPlot")
       )
     )
  )
))

# Maps https://code.highcharts.com/mapdata/
options(highcharter.download_map_data = FALSE)

# Define server logic required to draw a histogram
server <- function(input, output) {
  output$myMap <- renderHighchart({
    
    color_scale <- switch(input$chosen_variable,
                   "total_cases_per_million" =  c("#d3d3d3", "#520000"),
                   "total_deaths_per_million" = c("#d3d3d3", "#520000"),
                   "reproduction_rate" = c("#d3d3d3", "#61004f"),
                   "total_boosters_per_hundred" = c("#d3d3d3", "#005208"), 
                   "aged_65_older" = c("#d3d3d3", "#003752"))
    
    hcmap(
         input$chosen_continent, 
         data = data_wanted[data_wanted$date == input$date,],
         name = "Gross national income per capita", 
         value = input$chosen_variable,
         borderWidth = 0,
         nullColor = "#d3d3d3",
         joinBy = c("iso-a3", "iso_code")
     ) %>%
      hc_colorAxis(
        stops = color_stops(n = 10, colors = color_scale),
        type = "logarithmic"
      )
  })
  
  output$distPlot <- renderPlot({
    # generate bins based on input$bins from ui.R
    x    <- faithful[, 2]
    bins <- seq(min(x), max(x), length.out = input$bins + 1)
    
    # draw the histogram with the specified number of bins
    hist(x, breaks = bins, col = 'darkgray', border = 'white')
  })
  
}

#color_stops(colors = viridisLite::inferno(10, begin = 0.1))
# Run the application 
shinyApp(ui = ui, server = server)

